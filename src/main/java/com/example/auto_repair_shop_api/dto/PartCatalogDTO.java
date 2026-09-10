package com.example.auto_repair_shop_api.dto;

import java.math.BigDecimal;

public record PartCatalogDTO(
        Long id,
        String name,
        BigDecimal unitSellPrice
) {
}
