package com.monocept.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Reset token is required")
    private String token;

    // Same rule as registration and staff creation. Previously only
    // @Size(min = 8), so a direct API call could set "aaaaaaaa" or an
    // 80-character password (beyond bcrypt's 72-byte limit).
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-]).+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String newPassword;
}