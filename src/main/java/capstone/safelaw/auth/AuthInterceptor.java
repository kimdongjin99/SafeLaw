package capstone.safelaw.auth;

import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// "Authorization: Bearer <토큰>" 헤더를 검증하고, 회원 ID를 요청 속성(USER_ID)에 넣는다.
// 로그인이 필요한 경로는 WebConfig에서 지정한다.
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID = "authUserId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        request.setAttribute(USER_ID, jwtProvider.parseUserId(header.substring(BEARER_PREFIX.length()).trim()));
        return true;
    }
}
