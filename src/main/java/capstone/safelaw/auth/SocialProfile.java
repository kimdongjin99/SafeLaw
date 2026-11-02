package capstone.safelaw.auth;

/**
 * 검증을 통과한 소셜 ID 토큰에서 꺼낸 정보.
 * email은 제공자가 인증한 경우에만 채워지고, name은 토큰에 없으면 null이다.
 */
public record SocialProfile(String subject, String email, String name) {
}
