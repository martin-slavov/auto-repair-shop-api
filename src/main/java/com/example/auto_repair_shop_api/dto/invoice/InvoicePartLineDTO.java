package com.example.auto_repair_shop_api.dto.invoice;

import java.math.BigDecimal;

public record InvoicePartLineDTO(
        String partName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
