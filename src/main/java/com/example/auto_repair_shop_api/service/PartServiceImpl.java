package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.PartCatalogDTO;
import com.example.auto_repair_shop_api.dto.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.PartResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Part;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.PartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PartServiceImpl implements PartService {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private PartRepository partRepository;

    @Override
    @Transactional
    public PartResponseDTO createPart(PartCreateDTO dto) {

        if (partRepository.existsBySerialNumber(dto.serialNumber())) {
            throw new IllegalArgumentException("Part with this serial number already exists");
        }

        Part part = new Part();
        part.setName(dto.name());
        part.setSerialNumber(dto.serialNumber());
        part.setUnitCost(dto.unitCost());
        part.setUnitSellPrice(dto.unitSellPrice());

        Part createdPart = partRepository.save(part);

        return toResponseDto(createdPart);
    }

    @Override
    public List<?> getPartsForCurrentUser(String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        List<Part> parts = partRepository.findAll();

        if (appUser.getRole() == Role.ADMIN) {
            return parts.stream()
                    .map(this::toResponseDto)
                    .toList();
        }

        return parts.stream()
                .map(this::toCatalogDto)
                .toList();
    }

    private PartResponseDTO toResponseDto(Part part) {
        return new PartResponseDTO(
                part.getId(),
                part.getName(),
                part.getSerialNumber(),
                part.getUnitCost(),
                part.getUnitSellPrice()
        );
    }

    private PartCatalogDTO toCatalogDto(Part part) {
        return new PartCatalogDTO(
                part.getId(),
                part.getName(),
                part.getUnitSellPrice()
        );
    }
}
