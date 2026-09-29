package capstone.safelaw.controller;

import capstone.safelaw.auth.AuthInterceptor;
import capstone.safelaw.dto.ChangePasswordRequestDto;
import capstone.safelaw.dto.UpdateProfileRequestDto;
import capstone.safelaw.dto.UserDto;
import capstone.safelaw.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/users/** 는 로그인 토큰이 필요하다 (WebConfig → AuthInterceptor)
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "회원정보 API")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "앱 실행 시 저장된 토큰이 아직 유효한지 확인하는 용도로도 사용합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @Operation(summary = "내 정보 수정", description = "이름을 변경합니다.")
    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateMe(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                            @RequestBody UpdateProfileRequestDto request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @Operation(summary = "비밀번호 변경", description = "이메일로 가입한 회원만 사용할 수 있습니다.")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                               @RequestBody ChangePasswordRequestDto request) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴", description = "계정을 즉시 삭제합니다. 앱은 저장된 토큰과 기기 내 데이터를 함께 지우면 됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }
}
