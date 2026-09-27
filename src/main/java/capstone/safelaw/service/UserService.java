package capstone.safelaw.service;

import capstone.safelaw.domain.User;
import capstone.safelaw.dto.ChangePasswordRequestDto;
import capstone.safelaw.dto.UpdateProfileRequestDto;
import capstone.safelaw.dto.UserDto;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import capstone.safelaw.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// 로그인한 회원 본인의 정보 조회·수정·탈퇴
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDto getUser(long userId) {
        return UserDto.from(findUser(userId));
    }

    public UserDto updateProfile(long userId, UpdateProfileRequestDto request) {
        User user = findUser(userId);
        String name = AccountValidator.validateName(request.name());
        userRepository.updateName(user.id(), name);
        return UserDto.from(findUser(userId));
    }

    public void changePassword(long userId, ChangePasswordRequestDto request) {
        User user = findUser(userId);
        if (!user.hasPassword()) {
            throw new ApiException(ErrorCode.PASSWORD_NOT_SET);
        }
        String current = request.currentPassword() == null ? "" : request.currentPassword();
        if (!AccountValidator.isBcryptSafe(current) || !passwordEncoder.matches(current, user.passwordHash())) {
            throw new ApiException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        AccountValidator.validatePassword(request.newPassword());

        userRepository.updatePasswordHash(user.id(), passwordEncoder.encode(request.newPassword()));
        log.info("비밀번호 변경: userId={}", user.id());
    }

    // 회원 정보를 즉시 삭제한다. 상담 기록은 원래 기기에만 있으므로 서버에서 지울 것은 계정뿐이다.
    public void withdraw(long userId) {
        User user = findUser(userId);
        userRepository.deleteById(user.id());
        log.info("회원 탈퇴: userId={}", user.id());
    }

    // 토큰은 유효하지만 탈퇴한 회원이면 로그인 필요로 처리한다.
    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
