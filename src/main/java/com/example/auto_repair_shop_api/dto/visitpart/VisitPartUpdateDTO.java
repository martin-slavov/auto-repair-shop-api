package com.example.auto_repair_shop_api.dto.visitpart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VisitPartUpdateDTO(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity
) {
}
