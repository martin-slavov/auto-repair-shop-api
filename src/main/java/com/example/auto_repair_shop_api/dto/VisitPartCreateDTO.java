package com.example.auto_repair_shop_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VisitPartCreateDTO(
        @NotNull Long partId,
        @NotNull @Positive Integer quantity
) {
}
