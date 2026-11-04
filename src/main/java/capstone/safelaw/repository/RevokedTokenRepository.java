package capstone.safelaw.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

// 로그아웃한 토큰 목록. DB가 유출돼도 토큰을 쓸 수 없도록 원문 대신 SHA-256 해시만 저장한다.
@Repository
public class RevokedTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public RevokedTokenRepository(@Qualifier("userJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 만료된 토큰은 어차피 검증에서 걸러지므로, 등록할 때 목록에서 함께 지운다.
    public void revoke(String token, Instant expiresAt) {
        jdbcTemplate.update("DELETE FROM revoked_tokens WHERE expires_at < ?", Instant.now().getEpochSecond());
        jdbcTemplate.update("INSERT OR IGNORE INTO revoked_tokens (token_hash, expires_at) VALUES (?, ?)",
                hash(token), expiresAt.getEpochSecond());
    }

    public boolean isRevoked(String token) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revoked_tokens WHERE token_hash = ?", Integer.class, hash(token));
        return count != null && count > 0;
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
