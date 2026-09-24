package capstone.safelaw.exception;

import capstone.safelaw.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 개인정보 보호: 요청 본문(검색 벡터 등)이 로그에 섞이지 않도록 예외 메시지는 남기지 않는다.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        log.warn("요청 거부: {}", e.getErrorCode());
        return toResponse(e.getErrorCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson(HttpMessageNotReadableException e) {
        log.warn("요청 거부: {} ({})", ErrorCode.INVALID_JSON, e.getClass().getSimpleName());
        return toResponse(ErrorCode.INVALID_JSON);
    }

    // 없는 주소는 외부 스캐너 요청이 많으므로 로그를 남기지 않는다.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return toResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        // 그 밖의 Spring MVC 요청 오류(4xx)는 서버 오류로 취급하지 않는다.
        if (e instanceof org.springframework.web.ErrorResponse errorResponse
                && errorResponse.getStatusCode().is4xxClientError()) {
            log.warn("요청 거부: {} ({})", ErrorCode.INVALID_REQUEST, e.getClass().getSimpleName());
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
        }
        log.error("서버 내부 오류 발생", e);
        return toResponse(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }
}
