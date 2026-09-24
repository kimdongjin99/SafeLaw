package capstone.safelaw.controller;

import capstone.safelaw.config.ReleaseProperties;
import capstone.safelaw.dto.ReleaseInfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System & Updates", description = "온디바이스 AI 모델 업데이트 관련 API")
@RequiredArgsConstructor
public class SystemController {

    private final ReleaseProperties releaseProperties;

    @Operation(summary = "최신 AI 모델 배포 정보 조회", description = "온디바이스 답변 생성 LLM 파일의 버전, 주소, 크기, SHA-256을 반환합니다.")
    @GetMapping("/model/latest")
    public ResponseEntity<ReleaseInfoDto> getLatestModel() {
        return ResponseEntity.ok(releaseProperties.model());
    }
}
