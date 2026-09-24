package capstone.safelaw.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.File;

@Slf4j
@Configuration
public class SqliteConfig {

    // sqlite-jdbc는 파일이 없으면 빈 DB를 새로 만들어 버리므로, 연결 전에 존재 여부를 확인한다.
    @Bean
    public DataSource dataSource(@Value("${safelaw.sqlite.path}") String path) {
        File dbFile = new File(path);

        if (!dbFile.isFile()) {
            throw new IllegalStateException(
                    "SQLite 판례 DB 파일을 찾을 수 없습니다: " + dbFile.getAbsolutePath()
                            + " (safelaw.sqlite.path 설정을 확인하세요)");
        }

        log.info("SQLite DB 연결: {}", dbFile.getAbsolutePath());
        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:" + dbFile.getAbsolutePath())
                .build();
    }
}
