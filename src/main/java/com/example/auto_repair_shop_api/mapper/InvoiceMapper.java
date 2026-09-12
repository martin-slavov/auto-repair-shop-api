package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.model.Invoice;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceResponseDTO toResponseDto(Invoice invoice) {
        return new InvoiceResponseDTO(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getVisit().getVehicle().getLicensePlate(),
                invoice.getPartsPrice(),
                invoice.getLaborPrice(),
                invoice.getTotalPrice(),
                invoice.getIssuedDate(),
                invoice.isPaid()
        );
    }
}
