package com.example.auto_repair_shop_api.dto.part;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PartCreateDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Serial number is required")
        @Size(max = 60, message = "Serial number must not exceed 60 characters")
        String serialNumber,

        @NotNull(message = "Unit cost is required")
        @Positive(message = "Unit cost must be greater than zero")
        @Digits(integer = 6, fraction = 2, message = "Unit cost must have up to 6 digits and 2 decimal places")
        BigDecimal unitCost,

        @NotNull(message = "Unit sell price is required")
        @Positive(message = "Unit sell price must be greater than zero")
        @Digits(integer = 6, fraction = 2, message = "Unit sell price must have up to 6 digits and 2 decimal places")
        BigDecimal unitSellPrice
) {
}
