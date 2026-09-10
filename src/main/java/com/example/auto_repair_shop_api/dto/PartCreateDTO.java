package com.example.auto_repair_shop_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PartCreateDTO(
        @NotBlank String name,
        @NotBlank String serialNumber,
        @NotNull @Positive BigDecimal unitCost,
        @NotNull @Positive BigDecimal unitSellPrice
) {
}
