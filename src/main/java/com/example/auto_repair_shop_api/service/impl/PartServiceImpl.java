package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.part.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;
import com.example.auto_repair_shop_api.dto.part.PartUpdateDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.PartMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Part;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.PartRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.PartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PartServiceImpl implements PartService {

    private final AppUserService appUserService;
    private final PartRepository partRepository;
    private final PartMapper partMapper;
    private final VisitPartRepository visitPartRepository;

    @Autowired
    public PartServiceImpl(AppUserService appUserService, PartRepository partRepository, PartMapper partMapper,
                           VisitPartRepository visitPartRepository) {
        this.appUserService = appUserService;
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

    @Override
    @Transactional
    public PartResponseDTO updatePart(Long id, PartUpdateDTO dto, String currentUsername) {

        requireAdmin(currentUsername, "Only admins can update parts");
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
    public void deletePart(Long id, String currentUsername) {

        requireAdmin(currentUsername, "Only admins can delete parts");
        Part part = getPartOrThrow(id);

        if (visitPartRepository.existsByPartId(id)) {
            throw new ResourceInUseException("Part cannot be deleted because it is used in a service visit; deactivate it instead");
        }

        partRepository.delete(part);
    }

    private void requireAdmin(String currentUsername, String message) {
        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);
        if (appUser.getRole() != Role.ADMIN) {
            throw new OwnershipViolationException(message);
        }
    }

    private Part getPartOrThrow(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));
    }
}
