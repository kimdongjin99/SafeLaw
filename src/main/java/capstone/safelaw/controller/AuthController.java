package capstone.safelaw.controller;

import capstone.safelaw.auth.AuthInterceptor;
import capstone.safelaw.auth.SocialProvider;
import capstone.safelaw.dto.AuthResponseDto;
import capstone.safelaw.dto.LoginRequestDto;
import capstone.safelaw.dto.SignupRequestDto;
import capstone.safelaw.dto.SocialLoginRequestDto;
import capstone.safelaw.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "회원가입·로그인·로그아웃 API")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "가입 후 바로 로그인된 상태가 되도록 로그인 토큰을 함께 반환합니다.")
    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> signup(@RequestBody SignupRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    // 서버 앞에 프록시(로드밸런서 등)가 없으므로 접속 IP를 그대로 사용한다.
    @Operation(summary = "로그인", description = "이메일·비밀번호로 로그인하고 로그인 토큰을 반환합니다. 15분 내 실패가 많으면 429를 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    @Operation(summary = "Google 로그인", description = "앱에서 받은 Google ID 토큰으로 로그인합니다. 처음이면 termsAgreed=true일 때 가입됩니다.")
    @PostMapping("/google")
    public ResponseEntity<AuthResponseDto> google(@RequestBody SocialLoginRequestDto request) {
        return ResponseEntity.ok(authService.socialLogin(SocialProvider.GOOGLE, request));
    }

    @Operation(summary = "로그아웃", description = "요청에 사용한 토큰을 만료 전이어도 쓸 수 없게 합니다. 이 기기의 로그인만 풀리고, 앱은 저장된 토큰도 함께 지우면 됩니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                       @RequestAttribute(AuthInterceptor.TOKEN) String token) {
        authService.logout(userId, token);
        return ResponseEntity.noContent().build();
    }
}
