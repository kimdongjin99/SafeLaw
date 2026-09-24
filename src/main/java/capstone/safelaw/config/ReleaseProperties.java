package capstone.safelaw.config;

import capstone.safelaw.dto.ReleaseInfoDto;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.properties의 safelaw.release.* 값을 읽어온다.
@ConfigurationProperties(prefix = "safelaw.release")
public record ReleaseProperties(ReleaseInfoDto model, ReleaseInfoDto database) {
}
