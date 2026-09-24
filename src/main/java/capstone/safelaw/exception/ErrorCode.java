package capstone.safelaw.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_VECTOR(HttpStatus.BAD_REQUEST, "정확히 384차원의 유효한 숫자 벡터를 전송해주세요."),
    INVALID_TOP_K(HttpStatus.BAD_REQUEST, "topK는 1 이상 10 이하로 지정해주세요."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "요청 본문이 올바른 JSON 형식이 아닙니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type은 application/json이어야 합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
