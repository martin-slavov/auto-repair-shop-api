package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.invoice.InvoiceDetailDTO;
import com.example.auto_repair_shop_api.dto.invoice.InvoicePartLineDTO;
import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.model.Invoice;
import com.example.auto_repair_shop_api.model.VisitPart;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

    public InvoiceDetailDTO toDetailDto(Invoice invoice, List<VisitPart> parts,
                                        BigDecimal laborHours, BigDecimal laborRate) {
        return new InvoiceDetailDTO(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getVisit().getVehicle().getLicensePlate(),
                invoice.getPartsPrice(),
                invoice.getLaborPrice(),
                invoice.getTotalPrice(),
                invoice.getIssuedDate(),
                invoice.isPaid(),
                laborHours.setScale(2, RoundingMode.HALF_UP),
                laborRate,
                parts.stream().map(this::toPartLineDto).toList()
        );
    }

    private InvoicePartLineDTO toPartLineDto(VisitPart visitPart) {
        BigDecimal lineTotal = visitPart.getPriceAtTimeOfUse().setScale(2, RoundingMode.HALF_UP);
        BigDecimal unitPrice = lineTotal.divide(BigDecimal.valueOf(visitPart.getQuantity()), 2, RoundingMode.HALF_UP);
        return new InvoicePartLineDTO(
                visitPart.getPart().getName(),
                visitPart.getQuantity(),
                unitPrice,
                lineTotal
        );
    }
}
