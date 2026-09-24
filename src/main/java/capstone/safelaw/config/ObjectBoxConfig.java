package capstone.safelaw.config;

import capstone.safelaw.domain.MyObjectBox;
import io.objectbox.BoxStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class ObjectBoxConfig {

    // 서버 종료 시 DB 락(lock.mdb)을 안전하게 풀고 닫는다.
    @Bean(destroyMethod = "close")
    public BoxStore boxStore(@Value("${safelaw.objectbox.directory}") String directory) {
        File dbDirectory = new File(directory);

        // 폴더나 데이터 파일이 없으면 빈 DB가 새로 생성되므로, 서버 시작 자체를 실패시킨다.
        if (!dbDirectory.isDirectory() || !new File(dbDirectory, "data.mdb").isFile()) {
            throw new IllegalStateException(
                    "ObjectBox DB 폴더를 찾을 수 없습니다: " + dbDirectory.getAbsolutePath()
                            + " (data.mdb 포함 여부와 safelaw.objectbox.directory 설정을 확인하세요)");
        }

        log.info("ObjectBox DB 연결: {}", dbDirectory.getAbsolutePath());
        return MyObjectBox.builder().directory(dbDirectory).build();
    }
}
