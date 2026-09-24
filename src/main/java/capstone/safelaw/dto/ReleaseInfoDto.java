package capstone.safelaw.dto;

// 앱이 내려받을 파일(AI 모델, 판례 DB)의 배포 정보
public record ReleaseInfoDto(
        String version,
        String url,
        long sizeBytes,
        String sha256
) {
}
