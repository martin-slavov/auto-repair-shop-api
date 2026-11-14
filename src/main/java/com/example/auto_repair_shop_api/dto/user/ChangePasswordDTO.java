package com.example.auto_repair_shop_api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for changing the current user's own password.
 * newPassword uses the same rules as RegisterRequestDTO.password.
 */
public record ChangePasswordDTO(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String newPassword
) {
}
