package capstone.safelaw.dto;

public record SocialLoginRequestDto(
        String idToken,      // 앱에서 Google 로그인 후 받은 ID 토큰
        String name,         // 첫 가입 시 사용할 이름 (생략 가능)
        Boolean termsAgreed  // 첫 가입 시에만 필요. 이미 가입된 계정이면 무시
) {
}
