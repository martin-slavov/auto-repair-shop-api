package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.mapper.VisitPartMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Part;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.VisitPart;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.PartRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VisitPartServiceImpl implements VisitPartService {

    private final VisitPartRepository visitPartRepository;
    private final AppUserService appUserService;
    private final VisitAssignmentRepository visitAssignmentRepository;
    private final PartRepository partRepository;
    private final VisitPartMapper visitPartMapper;

    public VisitPartServiceImpl(VisitPartRepository visitPartRepository, AppUserService appUserService, VisitAssignmentRepository visitAssignmentRepository, PartRepository partRepository, VisitPartMapper visitPartMapper) {
        this.visitPartRepository = visitPartRepository;
        this.appUserService = appUserService;
        this.visitAssignmentRepository = visitAssignmentRepository;
        this.partRepository = partRepository;
        this.visitPartMapper = visitPartMapper;
    }

    @Override
    @Transactional
    public VisitPartResponseDTO addPartToVisit(Long visitId, VisitPartCreateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        VisitAssignment assignment = getAssignmentOrThrow(visitId, appUser.getId());

        if (assignment.getServiceVisit().getStatus() != VisitStatus.IN_PROGRESS) {
            throw new IllegalStateException("Parts can only be added while the visit is in progress");
        }

        Part part = partRepository.findById(dto.partId()).orElseThrow(() -> new IllegalArgumentException("Part not found"));

        VisitPart visitPart = new VisitPart();
        visitPart.setServiceVisit(assignment.getServiceVisit());
        visitPart.setPart(part);
        visitPart.setQuantity(dto.quantity());
        visitPart.setPriceAtTimeOfUse(part.getUnitSellPrice().multiply(BigDecimal.valueOf(dto.quantity())));

        VisitPart savedPart = visitPartRepository.save(visitPart);

        return visitPartMapper.toResponseDto(savedPart);
    }

    @Override
    public List<VisitPartResponseDTO> getPartsForVisit(Long visitId, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (appUser.getRole() == Role.MECHANIC) {
            getAssignmentOrThrow(visitId, appUser.getId());
        }

        List<VisitPart> parts = visitPartRepository.findByServiceVisitId(visitId);

        return parts.stream()
                .map(visitPartMapper::toResponseDto)
                .toList();
    }

    private VisitAssignment getAssignmentOrThrow(Long visitId, Long mechanicId) {
        return visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, mechanicId)
                .orElseThrow(() -> new SecurityException("You are not assigned to this visit"));
    }
}
