package com.example.auto_repair_shop_api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String username,
        @NotBlank String password
) {
}
