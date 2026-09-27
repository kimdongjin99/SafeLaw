package capstone.safelaw.dto;

import capstone.safelaw.domain.User;

import java.time.Instant;

/**
 * 앱에 내려주는 회원 정보 (비밀번호 해시는 절대 포함하지 않는다).
 * provider가 EMAIL이 아니면(소셜 로그인 회원) 비밀번호 변경 메뉴를 숨기면 된다. email은 null일 수 있다(Apple).
 */
public record UserDto(long id, String name, String email, String provider, Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(user.id(), user.name(), user.email(), user.provider(), user.createdAt());
    }
}
