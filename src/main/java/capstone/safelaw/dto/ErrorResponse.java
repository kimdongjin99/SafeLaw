package capstone.safelaw.dto;

import capstone.safelaw.exception.ErrorCode;

// 모든 에러 응답의 공통 형식: {"code": "...", "message": "..."}
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}
