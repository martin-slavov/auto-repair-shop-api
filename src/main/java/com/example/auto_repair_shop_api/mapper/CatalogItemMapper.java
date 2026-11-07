package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemResponseDTO;
import com.example.auto_repair_shop_api.model.CatalogItem;
import org.springframework.stereotype.Component;

@Component
public class CatalogItemMapper {

    public CatalogItemResponseDTO toResponseDto(CatalogItem service) {
        return new CatalogItemResponseDTO(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getBasePrice(),
                service.getActive(),
                service.getCreatedAt(),
                service.getEstimatedHours()
        );
    }
}