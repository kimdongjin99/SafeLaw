package capstone.safelaw.domain;

import java.time.Instant;

/**
 * 회원 DB(users 테이블)의 한 행. 비밀번호는 BCrypt 해시로만 저장한다.
 * 소셜 로그인 회원은 passwordHash가 null이고, Apple은 이메일을 주지 않을 수 있어 email도 null일 수 있다.
 */
public record User(
        long id,
        String name,
        String email,
        String passwordHash,
        String provider,       // EMAIL, GOOGLE, APPLE
        String providerUserId, // 소셜 로그인 회원의 Google/Apple 계정 고유 ID (sub)
        Instant createdAt
) {
    public static final String PROVIDER_EMAIL = "EMAIL";

    public boolean hasPassword() {
        return passwordHash != null;
    }
}
