package com.example.auto_repair_shop_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponseDTO(
        Long id,
        String invoiceNumber,
        String licensePlate,
        BigDecimal partsPrice,
        BigDecimal laborPrice,
        BigDecimal totalPrice,
        LocalDateTime issuedDate,
        boolean paid) {
}
