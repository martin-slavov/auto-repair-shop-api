package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.PartResponseDTO;
import com.example.auto_repair_shop_api.mapper.PartMapper;
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

    private final AppUserService appUserService;
    private final PartRepository partRepository;
    private final PartMapper partMapper;

    @Autowired
    public PartServiceImpl(AppUserService appUserService, PartRepository partRepository, PartMapper partMapper) {
        this.appUserService = appUserService;
        this.partRepository = partRepository;
        this.partMapper = partMapper;
    }

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

        return partMapper.toResponseDto(createdPart);
    }

    @Override
    public List<?> getPartsForCurrentUser(String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        List<Part> parts = partRepository.findAll();

        if (appUser.getRole() == Role.ADMIN) {
            return parts.stream()
                    .map(partMapper::toResponseDto)
                    .toList();
        }

        return parts.stream()
                .map(partMapper::toCatalogDto)
                .toList();
    }
}
