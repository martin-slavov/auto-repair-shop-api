package com.example.auto_repair_shop_api.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDetailDTO(
        Long id,
        String invoiceNumber,
        String licensePlate,
        BigDecimal partsPrice,
        BigDecimal laborPrice,
        BigDecimal totalPrice,
        LocalDateTime issuedDate,
        boolean paid,
        BigDecimal laborHours,
        BigDecimal laborRate,
        List<InvoicePartLineDTO> partLines) {
}
