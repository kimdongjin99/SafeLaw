package capstone.safelaw.config;

import capstone.safelaw.dto.ReleaseInfoDto;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.properties의 safelaw.release.* 값을 읽어온다.
// 임베딩 모델은 앱에 내장되고 판례 검색은 서버에서 하므로, 앱이 내려받는 파일은 답변 생성 LLM뿐이다.
@ConfigurationProperties(prefix = "safelaw.release")
public record ReleaseProperties(ReleaseInfoDto model) {
}
