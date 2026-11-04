package com.example.auto_repair_shop_api.dto.part;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PartCreateDTO(
        @NotBlank
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank
        @Size(max = 60, message = "Serial number must not exceed 60 characters")
        String serialNumber,

        @NotNull @Positive BigDecimal unitCost,
        @NotNull @Positive BigDecimal unitSellPrice
) {
}
