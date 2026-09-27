package capstone.safelaw.auth;

import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * 비밀번호 대입 공격 방지. 최근 15분 동안 로그인 실패가
 * 같은 이메일로 5회, 또는 같은 IP에서 20회를 넘으면 잠시 로그인을 막는다(429).
 * 서버 메모리에만 기록하므로 서버를 재시작하면 초기화된다.
 */
@Component
public class LoginAttemptLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILURES_PER_EMAIL = 5;
    private static final int MAX_FAILURES_PER_IP = 20;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    public synchronized void checkAllowed(String email, String ip) {
        Instant now = Instant.now();
        if (recentFailures(emailKey(email), now) >= MAX_FAILURES_PER_EMAIL
                || recentFailures(ipKey(ip), now) >= MAX_FAILURES_PER_IP) {
            throw new ApiException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    public synchronized void recordFailure(String email, String ip) {
        Instant now = Instant.now();
        if (failures.size() > CLEANUP_THRESHOLD) {
            failures.keySet().removeIf(key -> recentFailures(key, now) == 0);
        }
        failures.computeIfAbsent(emailKey(email), k -> new ArrayDeque<>()).addLast(now);
        failures.computeIfAbsent(ipKey(ip), k -> new ArrayDeque<>()).addLast(now);
    }

    // 로그인에 성공하면 그 이메일의 실패 기록만 지운다 (IP 기록은 다른 계정 대입 시도일 수 있어 유지)
    public synchronized void recordSuccess(String email) {
        failures.remove(emailKey(email));
    }

    private int recentFailures(String key, Instant now) {
        Deque<Instant> times = failures.get(key);
        if (times == null) {
            return 0;
        }
        Instant cutoff = now.minus(WINDOW);
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.pollFirst();
        }
        return times.size();
    }

    private static String emailKey(String email) {
        return "email:" + email;
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }
}
