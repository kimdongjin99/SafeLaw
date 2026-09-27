package capstone.safelaw.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.File;

// 회원 DB. 판례 DB는 업데이트 때 파일째 교체되므로 회원 정보는 별도 SQLite 파일에 둔다.
@Slf4j
@Configuration
public class UserDbConfig {

    @Bean
    public DataSource userDataSource(@Value("${safelaw.user-db.path}") String path) {
        File dbFile = new File(path).getAbsoluteFile();
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("회원 DB 폴더를 만들 수 없습니다: " + parent);
        }

        log.info("회원 DB 연결: {}", dbFile);
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:" + dbFile)
                .build();
        // SQLite는 동시 쓰기를 지원하지 않으므로 연결을 하나만 써서 SQLITE_BUSY를 피한다.
        dataSource.setMaximumPoolSize(1);
        dataSource.setPoolName("user-db");
        return dataSource;
    }

    @Bean
    public JdbcTemplate userJdbcTemplate(@Qualifier("userDataSource") DataSource userDataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(userDataSource);
        // 소셜 로그인 회원은 password_hash가 없고, Apple은 이메일을 주지 않을 수 있다.
        // SQLite의 UNIQUE는 NULL을 여러 개 허용한다.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    name             TEXT    NOT NULL,
                    email            TEXT    UNIQUE,
                    password_hash    TEXT,
                    provider         TEXT    NOT NULL,
                    provider_user_id TEXT,
                    created_at       TEXT    NOT NULL,
                    UNIQUE (provider, provider_user_id)
                )""");
        return jdbcTemplate;
    }
}
