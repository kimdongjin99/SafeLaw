package capstone.safelaw.auth;

import capstone.safelaw.config.SocialLoginProperties;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 앱이 Google/Apple 로그인 후 받은 ID 토큰을 검증한다.
 * 제공자가 공개한 서명 키(JWKS)로 서명을 확인하고, 발급자(iss)·발급 대상(aud)·만료 시간을 확인한다.
 * 공개 키는 캐시해두고, 모르는 키(kid)가 오면(제공자의 키 교체) 다시 받아온다.
 */
@Slf4j
@Component
public class SocialTokenVerifier {

    private static final Duration KEY_CACHE_TTL = Duration.ofHours(6);
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    private final Map<SocialProvider, List<String>> clientIds = new EnumMap<>(SocialProvider.class);
    private final Map<SocialProvider, KeySet> keySets = Collections.synchronizedMap(new EnumMap<>(SocialProvider.class));

    private record KeySet(Map<String, Key> keys, Instant fetchedAt) {
    }

    public SocialTokenVerifier(SocialLoginProperties properties) {
        clientIds.put(SocialProvider.GOOGLE, properties.googleClientIds());
        clientIds.put(SocialProvider.APPLE, properties.appleClientIds());
    }

    public SocialProfile verify(SocialProvider provider, String idToken) {
        List<String> audiences = clientIds.get(provider);
        if (audiences.isEmpty()) {
            throw new ApiException(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
        }
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return findKey(provider, header.getKeyId());
                        }
                    })
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("소셜 토큰 검증 실패: provider={} ({})", provider, e.getClass().getSimpleName());
            throw new ApiException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        Set<String> tokenAudiences = claims.getAudience();
        boolean audienceMatches = tokenAudiences != null && tokenAudiences.stream().anyMatch(audiences::contains);
        if (!provider.issuers().contains(claims.getIssuer())
                || !audienceMatches
                || claims.getExpiration() == null
                || claims.getSubject() == null || claims.getSubject().isBlank()) {
            log.warn("소셜 토큰 검증 실패: provider={} (iss/aud/exp/sub 불일치)", provider);
            throw new ApiException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        // 제공자가 인증한 이메일만 사용한다. Apple은 email_verified를 문자열 "true"로 줄 때가 있다.
        Object verified = claims.get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equals(verified);
        String email = claims.get("email", String.class);
        String normalizedEmail = emailVerified && email != null ? email.trim().toLowerCase(Locale.ROOT) : null;

        return new SocialProfile(claims.getSubject(), normalizedEmail, claims.get("name", String.class));
    }

    private Key findKey(SocialProvider provider, String keyId) {
        if (keyId == null) {
            throw new ApiException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }
        KeySet keySet = keySets.get(provider);
        if (keySet == null || isOlderThan(keySet, KEY_CACHE_TTL)
                || (!keySet.keys().containsKey(keyId) && isOlderThan(keySet, MIN_REFRESH_INTERVAL))) {
            keySet = refreshKeys(provider);
        }
        Key key = keySet.keys().get(keyId);
        if (key == null) {
            throw new ApiException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }
        return key;
    }

    // 동시에 여러 요청이 와도 한 번만 받아오도록 synchronized. 받아오기 실패 시 이전 키가 있으면 계속 사용한다.
    private synchronized KeySet refreshKeys(SocialProvider provider) {
        KeySet current = keySets.get(provider);
        if (current != null && !isOlderThan(current, MIN_REFRESH_INTERVAL)) {
            return current;
        }
        try {
            KeySet fetched = new KeySet(fetchKeys(provider), Instant.now());
            keySets.put(provider, fetched);
            log.info("소셜 로그인 공개키 갱신: provider={}, keys={}", provider, fetched.keys().size());
            return fetched;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("소셜 로그인 공개키를 받아오지 못했습니다: provider={}", provider, e);
            if (current != null) {
                return current;
            }
            throw new ApiException(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE);
        }
    }

    // 테스트에서 실제 Google/Apple 대신 로컬 키를 쓰도록 재정의할 수 있게 protected
    protected Map<String, Key> fetchKeys(SocialProvider provider) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(provider.jwksUri()))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("JWKS 응답 코드 " + response.statusCode());
        }

        JwkSet jwkSet = Jwks.setParser().build().parse(response.body());
        Map<String, Key> keys = new HashMap<>();
        for (Jwk<?> jwk : jwkSet.getKeys()) {
            if (jwk.getId() != null) {
                keys.put(jwk.getId(), jwk.toKey());
            }
        }
        return keys;
    }

    private static boolean isOlderThan(KeySet keySet, Duration age) {
        return keySet.fetchedAt().plus(age).isBefore(Instant.now());
    }
}
