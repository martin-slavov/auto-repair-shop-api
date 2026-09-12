package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.model.VisitPart;
import org.springframework.stereotype.Component;

@Component
public class VisitPartMapper {

    public VisitPartResponseDTO toResponseDto(VisitPart visitPart) {
        return new VisitPartResponseDTO(
                visitPart.getId(),
                visitPart.getPart().getName(),
                visitPart.getQuantity(),
                visitPart.getPriceAtTimeOfUse()
        );
    }
}
