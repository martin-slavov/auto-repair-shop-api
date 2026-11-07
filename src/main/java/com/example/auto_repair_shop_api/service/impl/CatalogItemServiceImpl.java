package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemCreateDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemResponseDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemUpdateDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.CatalogItemMapper;
import com.example.auto_repair_shop_api.model.CatalogItem;
import com.example.auto_repair_shop_api.repository.CatalogItemRepository;
import com.example.auto_repair_shop_api.service.CatalogItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogItemServiceImpl implements CatalogItemService {

    private final CatalogItemRepository catalogItemRepository;
    private final CatalogItemMapper catalogItemMapper;

    @Autowired
    public CatalogItemServiceImpl(CatalogItemRepository catalogItemRepository, CatalogItemMapper catalogItemMapper) {
        this.catalogItemRepository = catalogItemRepository;
        this.catalogItemMapper = catalogItemMapper;

    }

    @Override
    @Transactional
    public CatalogItemResponseDTO createCatalogItem(CatalogItemCreateDTO dto) {
        if (catalogItemRepository.existsByName(dto.name())) {
            throw new DuplicateResourceException("Catalog item with this name already exists");
        }

        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setName(dto.name());
        catalogItem.setDescription(dto.description());
        catalogItem.setBasePrice(dto.basePrice());
        catalogItem.setEstimatedHours(dto.estimatedHours());

        CatalogItem createdCatalogItem = catalogItemRepository.save(catalogItem);
        return catalogItemMapper.toResponseDto(createdCatalogItem);
    }

    @Override
    public List<CatalogItemResponseDTO> getAllCatalogItems() {
        List<CatalogItem> catalogItems = catalogItemRepository.findAll();

        return catalogItems.stream()
                .map(catalogItemMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public CatalogItemResponseDTO updateCatalogItem(Long id, CatalogItemUpdateDTO dto) {
        CatalogItem catalogItem = getCatalogItemOrThrow(id);

        if (catalogItemRepository.existsByNameAndIdNot(dto.name(), id)) {
            throw new DuplicateResourceException("Catalog item with this name already exists");
        }

        catalogItem.setName(dto.name());
        catalogItem.setDescription(dto.description());
        catalogItem.setBasePrice(dto.basePrice());
        catalogItem.setActive(dto.active());
        catalogItem.setEstimatedHours(dto.estimatedHours());

        CatalogItem updatedCatalogItem = catalogItemRepository.save(catalogItem);
        return catalogItemMapper.toResponseDto(updatedCatalogItem);
    }

    @Override
    public CatalogItemResponseDTO getCatalogItemById(Long id) {
        return catalogItemMapper.toResponseDto(getCatalogItemOrThrow(id));
    }

    @Override
    @Transactional
    public void deleteCatalogItem(Long id) {
        CatalogItem catalogItem = getCatalogItemOrThrow(id);
        catalogItemRepository.delete(catalogItem);
    }

    private CatalogItem getCatalogItemOrThrow(Long id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found"));
    }
}
