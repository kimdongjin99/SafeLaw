package capstone.safelaw.service;

import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

// 회원가입·정보 수정에서 공통으로 쓰는 입력값 검사
final class AccountValidator {

    private static final int MAX_NAME_LENGTH = 30;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 64;
    private static final int BCRYPT_MAX_BYTES = 72; // BCrypt는 72바이트까지만 사용한다
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private AccountValidator() {
    }

    static String validateName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(ErrorCode.INVALID_NAME);
        }
        return trimmed;
    }

    // 대소문자만 다른 이메일로 중복 가입하지 않도록 소문자로 저장한다.
    static String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(ErrorCode.INVALID_EMAIL);
        }
        return normalized;
    }

    static void validatePassword(String password) {
        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH
                || !isBcryptSafe(password)) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD);
        }
    }

    static boolean isBcryptSafe(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
