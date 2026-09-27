package capstone.safelaw.dto;

// 비밀번호 확인 입력은 앱에서 검사하므로 서버로 보내지 않는다.
public record SignupRequestDto(
        String name,
        String email,
        String password,
        Boolean termsAgreed // 필수 약관(온디바이스 AI 처리 안내 포함) 동의 여부, true여야 가입 가능
) {
}
