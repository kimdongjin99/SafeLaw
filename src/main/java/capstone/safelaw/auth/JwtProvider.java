package capstone.safelaw.auth;

import capstone.safelaw.config.JwtProperties;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

// 로그인 토큰(JWT, HS256) 발급·검증. 토큰에는 회원 ID만 담는다.
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration validity;

    public JwtProvider(JwtProperties properties) {
        this.validity = properties.accessTokenValidity();
        String secret = properties.secret();

        if (secret == null || secret.isBlank()) {
            log.warn("safelaw.auth.jwt.secret(SAFELAW_JWT_SECRET)이 없어 임의 키를 사용합니다. 서버를 재시작하면 기존 로그인이 모두 풀립니다.");
            this.key = Jwts.SIG.HS256.key().build();
            return;
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("safelaw.auth.jwt.secret은 32바이트 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    // 서명이 틀렸거나 만료된 토큰이면 UNAUTHORIZED
    public long parseUserId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.parseLong(subject);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    public long getValiditySeconds() {
        return validity.toSeconds();
    }
}
