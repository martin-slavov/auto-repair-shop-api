package com.example.auto_repair_shop_api.dto.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserEnabledDTO(
        @NotNull(message = "Enabled is required")
        Boolean enabled
) {
}
