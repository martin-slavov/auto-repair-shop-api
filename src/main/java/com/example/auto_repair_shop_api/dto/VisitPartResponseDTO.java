package com.example.auto_repair_shop_api.dto;

import java.math.BigDecimal;

public record VisitPartResponseDTO(Long id, String partName, Integer quantity, BigDecimal priceAtTimeOfUse) {
}
