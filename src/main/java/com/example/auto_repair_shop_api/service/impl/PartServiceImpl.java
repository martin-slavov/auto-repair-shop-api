package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.part.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;
import com.example.auto_repair_shop_api.dto.part.PartUpdateDTO;
import com.example.auto_repair_shop_api.dto.part.PartView;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.PartMapper;
import com.example.auto_repair_shop_api.model.Part;
import com.example.auto_repair_shop_api.repository.PartRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import com.example.auto_repair_shop_api.service.PartService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.example.auto_repair_shop_api.repository.specification.PartSpecifications.hasActive;
import static com.example.auto_repair_shop_api.repository.specification.PartSpecifications.nameContains;
import static com.example.auto_repair_shop_api.repository.specification.PartSpecifications.sellPriceAtLeast;
import static com.example.auto_repair_shop_api.repository.specification.PartSpecifications.sellPriceAtMost;
import static com.example.auto_repair_shop_api.repository.specification.PartSpecifications.serialNumberContains;

@Service
public class PartServiceImpl implements PartService {

    // Sort whitelist for everyone: API field name -> entity property.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "name", "name",
            "serialNumber", "serialNumber",
            "unitSellPrice", "unitSellPrice"
    );
    private static final Map<String, String> ADMIN_SORT_FIELDS = withEntry(SORT_FIELDS, "unitCost", "unitCost");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final PartRepository partRepository;
    private final PartMapper partMapper;
    private final VisitPartRepository visitPartRepository;

    @Autowired
    public PartServiceImpl(PartRepository partRepository, PartMapper partMapper,
                           VisitPartRepository visitPartRepository) {
        this.partRepository = partRepository;
        this.partMapper = partMapper;
        this.visitPartRepository = visitPartRepository;
    }

    @Override
    @Transactional
    public PartResponseDTO createPart(PartCreateDTO dto) {

        if (partRepository.existsBySerialNumber(dto.serialNumber())) {
            throw new DuplicateResourceException("Part with this serial number already exists");
        }

        Part part = new Part();
        part.setName(dto.name());
        part.setSerialNumber(dto.serialNumber());
        part.setUnitCost(dto.unitCost());
        part.setUnitSellPrice(dto.unitSellPrice());

        Part createdPart = partRepository.save(part);

        return partMapper.toResponseDto(createdPart);
    }

    @Override
    public PageResponse<PartView> getParts(String name, String serialNumber, BigDecimal minPrice, BigDecimal maxPrice,
                                           Boolean active, boolean isAdmin,
                                           Integer page, Integer size, String sort) {

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not be greater than maxPrice");
        }

        PageRequest pageRequest = PageRequestFactory.create(
                page, size, sort, isAdmin ? ADMIN_SORT_FIELDS : SORT_FIELDS, DEFAULT_SORT);

        List<Specification<Part>> filters = new ArrayList<>();

        if (!isAdmin) {
            filters.add(hasActive(true));
        } else if (active != null) {
            filters.add(hasActive(active));
        }

        if (name != null && !name.isBlank()) {
            filters.add(nameContains(name));
        }
        if (serialNumber != null && !serialNumber.isBlank()) {
            filters.add(serialNumberContains(serialNumber));
        }
        if (minPrice != null) {
            filters.add(sellPriceAtLeast(minPrice));
        }
        if (maxPrice != null) {
            filters.add(sellPriceAtMost(maxPrice));
        }

        Page<Part> result = partRepository.findAll(Specification.allOf(filters), pageRequest);

        Function<Part, PartView> mapper = isAdmin ? partMapper::toResponseDto : partMapper::toCatalogDto;
        return PageResponse.from(result, mapper);
    }

    @Override
    @Transactional
    public PartResponseDTO updatePart(Long id, PartUpdateDTO dto) {

        Part part = getPartOrThrow(id);

        if (partRepository.existsBySerialNumberAndIdNot(dto.serialNumber(), id)) {
            throw new DuplicateResourceException("Part with this serial number already exists");
        }

        part.setName(dto.name());
        part.setSerialNumber(dto.serialNumber());
        part.setUnitCost(dto.unitCost());
        part.setUnitSellPrice(dto.unitSellPrice());
        part.setActive(dto.active());

        Part updatedPart = partRepository.save(part);
        return partMapper.toResponseDto(updatedPart);
    }

    @Override
    @Transactional
    public void deletePart(Long id) {

        Part part = getPartOrThrow(id);

        if (visitPartRepository.existsByPartId(id)) {
            throw new ResourceInUseException("Part cannot be deleted because it is used in a service visit; deactivate it instead");
        }

        partRepository.delete(part);
    }

    private Part getPartOrThrow(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));
    }

    private static Map<String, String> withEntry(Map<String, String> base, String key, String value) {
        Map<String, String> copy = new HashMap<>(base);
        copy.put(key, value);
        return Map.copyOf(copy);
    }
}
