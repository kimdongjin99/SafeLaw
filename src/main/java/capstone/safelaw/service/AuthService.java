package capstone.safelaw.service;

import capstone.safelaw.auth.JwtProvider;
import capstone.safelaw.auth.LoginAttemptLimiter;
import capstone.safelaw.auth.SocialProfile;
import capstone.safelaw.auth.SocialProvider;
import capstone.safelaw.auth.SocialTokenVerifier;
import capstone.safelaw.domain.User;
import capstone.safelaw.dto.AuthResponseDto;
import capstone.safelaw.dto.LoginRequestDto;
import capstone.safelaw.dto.SignupRequestDto;
import capstone.safelaw.dto.SocialLoginRequestDto;
import capstone.safelaw.dto.UserDto;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import capstone.safelaw.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

// 개인정보 보호: 이메일·이름·비밀번호는 로그에 남기지 않고 회원 ID만 남긴다.
@Slf4j
@Service
public class AuthService {

    private static final String DEFAULT_SOCIAL_NAME = "사용자";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final SocialTokenVerifier socialTokenVerifier;
    // 없는 이메일로 로그인해도 비밀번호 검사 시간을 똑같이 써서, 응답 시간으로 가입 여부를 알 수 없게 한다.
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, JwtProvider jwtProvider, PasswordEncoder passwordEncoder,
                       LoginAttemptLimiter loginAttemptLimiter, SocialTokenVerifier socialTokenVerifier) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.socialTokenVerifier = socialTokenVerifier;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public AuthResponseDto signup(SignupRequestDto request) {
        String name = AccountValidator.validateName(request.name());
        String email = AccountValidator.normalizeEmail(request.email());
        AccountValidator.validatePassword(request.password());
        if (!Boolean.TRUE.equals(request.termsAgreed())) {
            throw new ApiException(ErrorCode.TERMS_NOT_AGREED);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user;
        try {
            user = userRepository.save(name, email, passwordEncoder.encode(request.password()));
        } catch (DataAccessException e) {
            // 같은 이메일로 동시에 가입한 경우 UNIQUE 제약에 걸린다.
            if (userRepository.existsByEmail(email)) {
                throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            throw e;
        }

        log.info("회원가입 완료: userId={}", user.id());
        return issueToken(user);
    }

    public AuthResponseDto login(LoginRequestDto request, String clientIp) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password() == null ? "" : request.password();
        loginAttemptLimiter.checkAllowed(email, clientIp);

        // 소셜 로그인 회원(비밀번호 없음)도 없는 회원과 똑같이 처리한다.
        Optional<User> user = userRepository.findByEmail(email).filter(User::hasPassword);
        String hash = user.map(User::passwordHash).orElse(dummyPasswordHash);
        boolean matches = AccountValidator.isBcryptSafe(password) && passwordEncoder.matches(password, hash);

        if (user.isEmpty() || !matches) {
            loginAttemptLimiter.recordFailure(email, clientIp);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        loginAttemptLimiter.recordSuccess(email);
        log.info("로그인: userId={}", user.get().id());
        return issueToken(user.get());
    }

    /**
     * Google/Apple 로그인. 이미 가입된 소셜 계정이면 로그인하고, 처음이면 가입시킨다.
     * 처음인데 termsAgreed가 true가 아니면 TERMS_NOT_AGREED → 앱에서 약관 동의를 받은 뒤 같은 ID 토큰으로 다시 요청한다.
     */
    public AuthResponseDto socialLogin(SocialProvider provider, SocialLoginRequestDto request) {
        SocialProfile profile = socialTokenVerifier.verify(provider, request.idToken());

        Optional<User> existing = userRepository.findByProvider(provider.name(), profile.subject());
        if (existing.isPresent()) {
            log.info("소셜 로그인: provider={}, userId={}", provider, existing.get().id());
            return issueToken(existing.get());
        }

        if (!Boolean.TRUE.equals(request.termsAgreed())) {
            throw new ApiException(ErrorCode.TERMS_NOT_AGREED);
        }
        // 같은 이메일의 다른 계정과 자동으로 합치지 않는다 (남의 계정에 로그인되는 사고 방지).
        if (profile.email() != null && userRepository.existsByEmail(profile.email())) {
            throw new ApiException(ErrorCode.EMAIL_REGISTERED_WITH_OTHER_METHOD);
        }

        User user;
        try {
            user = userRepository.saveSocial(resolveSocialName(request.name(), profile.name()),
                    profile.email(), provider.name(), profile.subject());
        } catch (DataAccessException e) {
            // 같은 계정으로 동시에 첫 로그인한 경우
            Optional<User> raced = userRepository.findByProvider(provider.name(), profile.subject());
            if (raced.isPresent()) {
                return issueToken(raced.get());
            }
            if (profile.email() != null && userRepository.existsByEmail(profile.email())) {
                throw new ApiException(ErrorCode.EMAIL_REGISTERED_WITH_OTHER_METHOD);
            }
            throw e;
        }

        log.info("소셜 회원가입 완료: provider={}, userId={}", provider, user.id());
        return issueToken(user);
    }

    private AuthResponseDto issueToken(User user) {
        return AuthResponseDto.bearer(
                jwtProvider.createAccessToken(user.id()),
                jwtProvider.getValiditySeconds(),
                UserDto.from(user));
    }

    // 앱이 보낸 이름 → 토큰의 이름(Google) → 기본값 순. 이름은 나중에 설정에서 바꿀 수 있다.
    private static String resolveSocialName(String requestedName, String tokenName) {
        for (String candidate : new String[]{requestedName, tokenName}) {
            if (candidate != null && !candidate.isBlank()) {
                return AccountValidator.validateName(candidate);
            }
        }
        return DEFAULT_SOCIAL_NAME;
    }
}
