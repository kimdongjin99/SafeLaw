package capstone.safelaw.repository;

import capstone.safelaw.domain.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

// 회원 DB는 판례 DB(JPA)와 별도 파일이라 JdbcTemplate으로 직접 조회한다.
@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("provider"),
            rs.getString("provider_user_id"),
            Instant.parse(rs.getString("created_at")));

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(@Qualifier("userJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_ROW_MAPPER, id)
                .stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query("SELECT * FROM users WHERE email = ?", USER_ROW_MAPPER, email)
                .stream().findFirst();
    }

    public Optional<User> findByProvider(String provider, String providerUserId) {
        return jdbcTemplate.query("SELECT * FROM users WHERE provider = ? AND provider_user_id = ?",
                        USER_ROW_MAPPER, provider, providerUserId)
                .stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    // 이메일 회원가입
    public User save(String name, String email, String passwordHash) {
        return insert(name, email, passwordHash, User.PROVIDER_EMAIL, null);
    }

    // 소셜 로그인 첫 가입 (비밀번호 없음)
    public User saveSocial(String name, String email, String provider, String providerUserId) {
        return insert(name, email, null, provider, providerUserId);
    }

    public void updateName(long id, String name) {
        jdbcTemplate.update("UPDATE users SET name = ? WHERE id = ?", name, id);
    }

    public void updatePasswordHash(long id, String passwordHash) {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    public void deleteById(long id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    private User insert(String name, String email, String passwordHash, String provider, String providerUserId) {
        Instant createdAt = Instant.now();
        Long id = jdbcTemplate.queryForObject("""
                        INSERT INTO users (name, email, password_hash, provider, provider_user_id, created_at)
                        VALUES (?, ?, ?, ?, ?, ?) RETURNING id""",
                Long.class, name, email, passwordHash, provider, providerUserId, createdAt.toString());
        return new User(id, name, email, passwordHash, provider, providerUserId, createdAt);
    }
}
