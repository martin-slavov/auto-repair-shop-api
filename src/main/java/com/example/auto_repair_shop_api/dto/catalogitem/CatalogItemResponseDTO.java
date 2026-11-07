package com.example.auto_repair_shop_api.dto.catalogitem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CatalogItemResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        Boolean active,
        LocalDateTime createdAt,
        BigDecimal estimatedHours
) {
}