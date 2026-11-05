package com.example.auto_repair_shop_api.dto.visitpart;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VisitPartResponseDTO(Long id, String partName, Integer quantity, BigDecimal priceAtTimeOfUse,
                                   LocalDateTime addedAt) {
}
