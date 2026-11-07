package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemCreateDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemResponseDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemUpdateDTO;

import java.util.List;

public interface CatalogItemService {
    CatalogItemResponseDTO createCatalogItem(CatalogItemCreateDTO dto);

    List<CatalogItemResponseDTO> getAllCatalogItems();

    CatalogItemResponseDTO updateCatalogItem(Long id, CatalogItemUpdateDTO dto);

    CatalogItemResponseDTO getCatalogItemById(Long id);

    void deleteCatalogItem(Long id);
}
