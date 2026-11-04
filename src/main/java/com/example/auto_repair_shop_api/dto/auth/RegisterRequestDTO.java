package com.example.auto_repair_shop_api.dto.auth;

import com.example.auto_repair_shop_api.validation.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordMatch
public record RegisterRequestDTO(
        @NotBlank(message = "Username is required")
        @Size(min = 1, max = 50, message = "Username must be between 1 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password,

        @NotBlank(message = "Confirm password is required")
        @Size(max = 255, message = "Confirm password must not exceed 255 characters")
        String confirmPassword
) {
}
