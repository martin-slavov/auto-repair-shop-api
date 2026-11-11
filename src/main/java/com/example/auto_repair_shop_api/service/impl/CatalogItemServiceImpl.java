package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemCreateDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemResponseDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.CatalogItemMapper;
import com.example.auto_repair_shop_api.model.CatalogItem;
import com.example.auto_repair_shop_api.repository.CatalogItemRepository;
import com.example.auto_repair_shop_api.service.CatalogItemService;
import com.example.auto_repair_shop_api.util.PageRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.auto_repair_shop_api.repository.specification.CatalogItemSpecifications.hasActive;
import static com.example.auto_repair_shop_api.repository.specification.CatalogItemSpecifications.nameContains;
import static com.example.auto_repair_shop_api.repository.specification.CatalogItemSpecifications.priceAtLeast;
import static com.example.auto_repair_shop_api.repository.specification.CatalogItemSpecifications.priceAtMost;

@Service
public class CatalogItemServiceImpl implements CatalogItemService {

    // Sort whitelist: API field name -> entity property.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "name", "name",
            "price", "basePrice",
            "estimatedHours", "estimatedHours",
            "createdAt", "createdAt"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

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
    public PageResponse<CatalogItemResponseDTO> getAllCatalogItems(String name, BigDecimal minPrice, BigDecimal maxPrice,
                                                                   Boolean active, boolean isAdmin,
                                                                   Integer page, Integer size, String sort) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not be greater than maxPrice");
        }

        PageRequest pageRequest = PageRequestFactory.create(page, size, sort, SORT_FIELDS, DEFAULT_SORT);

        List<Specification<CatalogItem>> filters = new ArrayList<>();

        if (!isAdmin) {
            filters.add(hasActive(true));
        } else if (active != null) {
            filters.add(hasActive(active));
        }

        if (name != null && !name.isBlank()) {
            filters.add(nameContains(name));
        }
        if (minPrice != null) {
            filters.add(priceAtLeast(minPrice));
        }
        if (maxPrice != null) {
            filters.add(priceAtMost(maxPrice));
        }

        Page<CatalogItem> result = catalogItemRepository.findAll(Specification.allOf(filters), pageRequest);
        return PageResponse.from(result, catalogItemMapper::toResponseDto);
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
    public CatalogItemResponseDTO getCatalogItemById(Long id, boolean isAdmin) {
        CatalogItem catalogItem = getCatalogItemOrThrow(id);

        if (!isAdmin && !Boolean.TRUE.equals(catalogItem.getActive())) {
            throw new ResourceNotFoundException("Catalog item not found");
        }

        return catalogItemMapper.toResponseDto(catalogItem);
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
