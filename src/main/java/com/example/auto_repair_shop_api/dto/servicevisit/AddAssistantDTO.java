package com.example.auto_repair_shop_api.dto.servicevisit;

import jakarta.validation.constraints.NotNull;

public record AddAssistantDTO(
        @NotNull(message = "Mechanic ID is required")
        Long mechanicId
) {
}
