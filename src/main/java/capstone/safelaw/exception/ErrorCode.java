package capstone.safelaw.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_VECTOR(HttpStatus.BAD_REQUEST, "정확히 384차원의 유효한 숫자 벡터를 전송해주세요."),
    INVALID_TOP_K(HttpStatus.BAD_REQUEST, "topK는 1 이상 10 이하로 지정해주세요."),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "이름은 1자 이상 30자 이하로 입력해주세요."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "올바른 이메일 주소를 입력해주세요."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상 64자 이하로 입력해주세요."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해주세요."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    EMAIL_REGISTERED_WITH_OTHER_METHOD(HttpStatus.CONFLICT, "이미 다른 방법으로 가입된 이메일입니다. 기존 방법으로 로그인해주세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요하거나 로그인이 만료되었습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST, "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "소셜 로그인 정보가 올바르지 않거나 만료되었습니다. 다시 시도해주세요."),
    SOCIAL_LOGIN_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "현재 사용할 수 없는 로그인 방식입니다."),
    SOCIAL_LOGIN_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "요청 본문이 올바른 JSON 형식이 아닙니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type은 application/json이어야 합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
