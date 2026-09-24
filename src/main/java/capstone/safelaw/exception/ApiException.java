package capstone.safelaw.exception;

import lombok.Getter;

// 클라이언트에게 ErrorCode 그대로 전달할 예외
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
