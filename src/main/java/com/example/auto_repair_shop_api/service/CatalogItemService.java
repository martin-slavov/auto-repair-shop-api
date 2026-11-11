package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemCreateDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemResponseDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;

import java.math.BigDecimal;

public interface CatalogItemService {
    CatalogItemResponseDTO createCatalogItem(CatalogItemCreateDTO dto);

    PageResponse<CatalogItemResponseDTO> getAllCatalogItems(String name, BigDecimal minPrice, BigDecimal maxPrice,
                                                            Boolean active, boolean isAdmin,
                                                            Integer page, Integer size, String sort);

    CatalogItemResponseDTO updateCatalogItem(Long id, CatalogItemUpdateDTO dto);

    CatalogItemResponseDTO getCatalogItemById(Long id, boolean isAdmin);

    void deleteCatalogItem(Long id);
}
