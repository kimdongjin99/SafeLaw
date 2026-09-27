package capstone.safelaw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// application.properties의 safelaw.auth.jwt.* 값을 읽어온다.
@ConfigurationProperties(prefix = "safelaw.auth.jwt")
public record JwtProperties(String secret, Duration accessTokenValidity) {
}
