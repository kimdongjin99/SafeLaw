package capstone.safelaw.dto;

/**
 * 회원가입·로그인 성공 응답.
 * 앱은 accessToken을 기기에 저장해두고, 로그인이 필요한 API 호출 시 "Authorization: Bearer {accessToken}" 헤더로 보낸다.
 * 로그아웃은 앱에서 저장한 토큰을 지우면 된다.
 */
public record AuthResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn, // 토큰 유효 시간(초)
        UserDto user
) {
    public static AuthResponseDto bearer(String accessToken, long expiresIn, UserDto user) {
        return new AuthResponseDto(accessToken, "Bearer", expiresIn, user);
    }
}
