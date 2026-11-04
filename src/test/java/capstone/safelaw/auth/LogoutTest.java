package capstone.safelaw.auth;

import capstone.safelaw.config.JwtProperties;
import capstone.safelaw.config.UserDbConfig;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import capstone.safelaw.repository.RevokedTokenRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 임시 SQLite 파일에 회원 DB 테이블을 실제로 만들어 로그아웃 토큰 차단을 확인한다.
class LogoutTest {

    @TempDir
    Path tempDir;

    private final JwtProvider jwtProvider =
            new JwtProvider(new JwtProperties("test-secret-test-secret-test-secret-1234", Duration.ofDays(7)));
    private HikariDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private RevokedTokenRepository repository;
    private AuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        UserDbConfig config = new UserDbConfig();
        dataSource = (HikariDataSource) config.userDataSource(tempDir.resolve("users.db").toString());
        jdbcTemplate = config.userJdbcTemplate(dataSource);
        repository = new RevokedTokenRepository(jdbcTemplate);
        interceptor = new AuthInterceptor(jwtProvider, repository);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void validTokenPassesAndExposesUserIdAndToken() {
        String token = jwtProvider.createAccessToken(7L);

        MockHttpServletRequest request = requestWith(token);
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        assertEquals(7L, request.getAttribute(AuthInterceptor.USER_ID));
        assertEquals(token, request.getAttribute(AuthInterceptor.TOKEN));
    }

    @Test
    void revokedTokenIsRejectedButOtherDeviceTokenStillWorks() {
        String loggedOut = jwtProvider.createAccessToken(7L);
        String otherDevice = jwtProvider.createAccessToken(7L);
        assertNotEquals(loggedOut, otherDevice);

        repository.revoke(loggedOut, jwtProvider.getExpiration(loggedOut));

        ApiException e = assertThrows(ApiException.class,
                () -> interceptor.preHandle(requestWith(loggedOut), new MockHttpServletResponse(), new Object()));
        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
        assertTrue(interceptor.preHandle(requestWith(otherDevice), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void revokingTwiceIsHarmless() {
        Instant expiresAt = Instant.now().plusSeconds(60);

        repository.revoke("token", expiresAt);
        repository.revoke("token", expiresAt);

        assertTrue(repository.isRevoked("token"));
    }

    @Test
    void revokeRemovesExpiredEntries() {
        repository.revoke("old-token", Instant.now().minusSeconds(60));
        assertTrue(repository.isRevoked("old-token"));

        repository.revoke("new-token", Instant.now().plusSeconds(60));

        assertFalse(repository.isRevoked("old-token"));
        assertTrue(repository.isRevoked("new-token"));
    }

    @Test
    void tokenIsNotStoredInPlainText() {
        repository.revoke("plain-token", Instant.now().plusSeconds(60));

        String stored = jdbcTemplate.queryForObject("SELECT token_hash FROM revoked_tokens", String.class);
        assertNotEquals("plain-token", stored);
        assertEquals(64, stored.length());
    }

    private static MockHttpServletRequest requestWith(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
