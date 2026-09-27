package capstone.safelaw.dto;

public record ChangePasswordRequestDto(String currentPassword, String newPassword) {
}
