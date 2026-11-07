package com.example.auto_repair_shop_api.dto.catalogitem;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CatalogItemCreateDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Base price is required")
        @PositiveOrZero(message = "Base price must not be negative")
        @Digits(integer = 6, fraction = 2, message = "Base price must have up to 6 digits and 2 decimal places")
        BigDecimal basePrice,

        @NotNull(message = "Estimated hours are required")
        @Positive(message = "Estimated hours must be greater than zero")
        @Digits(integer = 3, fraction = 2, message = "Estimated hours must have up to 3 digits and 2 decimal places")
        BigDecimal estimatedHours
) {
}