package com.example.auto_repair_shop_api.dto;

import java.math.BigDecimal;

public record PartResponseDTO(
        Long id,
        String name,
        String serialNumber,
        BigDecimal unitCost,
        BigDecimal unitSellPrice
) {
}
