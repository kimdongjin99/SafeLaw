package capstone.safelaw.auth;

import capstone.safelaw.config.SocialLoginProperties;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 실제 Google 서버 대신 로컬에서 만든 RSA 키로 서명한 토큰으로 검증 로직을 확인한다.
class SocialTokenVerifierTest {

    private static final String CLIENT_ID = "test-client.apps.googleusercontent.com";
    private static final String KEY_ID = "test-kid";

    private final KeyPair keyPair = Jwts.SIG.RS256.keyPair().build();
    private final SocialTokenVerifier verifier = verifierWith(List.of(CLIENT_ID));

    @Test
    void validToken() {
        SocialProfile profile = verifier.verify(SocialProvider.GOOGLE, token(keyPair, CLIENT_ID, "https://accounts.google.com", 3600, true));

        assertEquals("google-user-1", profile.subject());
        assertEquals("user@gmail.com", profile.email());
        assertEquals("홍길동", profile.name());
    }

    @Test
    void unverifiedEmailIsIgnored() {
        SocialProfile profile = verifier.verify(SocialProvider.GOOGLE, token(keyPair, CLIENT_ID, "accounts.google.com", 3600, false));

        assertNull(profile.email());
    }

    @Test
    void rejectsOtherAudience() {
        assertError(ErrorCode.INVALID_SOCIAL_TOKEN, () ->
                verifier.verify(SocialProvider.GOOGLE, token(keyPair, "other-app", "https://accounts.google.com", 3600, true)));
    }

    @Test
    void rejectsOtherIssuer() {
        assertError(ErrorCode.INVALID_SOCIAL_TOKEN, () ->
                verifier.verify(SocialProvider.GOOGLE, token(keyPair, CLIENT_ID, "https://evil.example.com", 3600, true)));
    }

    @Test
    void rejectsExpiredToken() {
        assertError(ErrorCode.INVALID_SOCIAL_TOKEN, () ->
                verifier.verify(SocialProvider.GOOGLE, token(keyPair, CLIENT_ID, "https://accounts.google.com", -600, true)));
    }

    @Test
    void rejectsTokenSignedWithOtherKey() {
        KeyPair attacker = Jwts.SIG.RS256.keyPair().build();
        assertError(ErrorCode.INVALID_SOCIAL_TOKEN, () ->
                verifier.verify(SocialProvider.GOOGLE, token(attacker, CLIENT_ID, "https://accounts.google.com", 3600, true)));
    }

    @Test
    void rejectsGarbage() {
        assertError(ErrorCode.INVALID_SOCIAL_TOKEN, () -> verifier.verify(SocialProvider.GOOGLE, "not-a-token"));
    }

    @Test
    void notConfiguredProvider() {
        assertError(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED, () -> verifier.verify(SocialProvider.APPLE, "anything"));
    }

    private SocialTokenVerifier verifierWith(List<String> googleClientIds) {
        return new SocialTokenVerifier(new SocialLoginProperties(googleClientIds, List.of())) {
            @Override
            protected Map<String, Key> fetchKeys(SocialProvider provider) {
                return Map.of(KEY_ID, keyPair.getPublic());
            }
        };
    }

    private static String token(KeyPair signer, String audience, String issuer, long expiresInSeconds, boolean emailVerified) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject("google-user-1")
                .claim("email", "User@Gmail.com")
                .claim("email_verified", emailVerified)
                .claim("name", "홍길동")
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(signer.getPrivate())
                .compact();
    }

    private static void assertError(ErrorCode expected, Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(expected, e.getErrorCode());
    }
}
