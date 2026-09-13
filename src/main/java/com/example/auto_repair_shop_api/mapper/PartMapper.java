package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.part.PartCatalogDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;
import com.example.auto_repair_shop_api.model.Part;
import org.springframework.stereotype.Component;

@Component
public class PartMapper {

    public PartResponseDTO toResponseDto(Part part) {
        return new PartResponseDTO(
                part.getId(),
                part.getName(),
                part.getSerialNumber(),
                part.getUnitCost(),
                part.getUnitSellPrice()
        );
    }

    public PartCatalogDTO toCatalogDto(Part part) {
        return new PartCatalogDTO(
                part.getId(),
                part.getName(),
                part.getUnitSellPrice()
        );
    }
}
