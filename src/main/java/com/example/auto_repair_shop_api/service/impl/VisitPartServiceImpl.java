package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.visitpart.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartUpdateDTO;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
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
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.VisitPartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            throw new InvalidStateTransitionException("Parts can only be added while the visit is in progress");
        }

        Part part = partRepository.findById(dto.partId()).orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        if (!Boolean.TRUE.equals(part.getActive())) {
            throw new IllegalArgumentException("Part is not active");
        }

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

    @Override
    @Transactional
    public VisitPartResponseDTO updateVisitPart(Long visitId, Long partLineId, VisitPartUpdateDTO dto, String currentUsername) {

        VisitPart visitPart = getEditablePartLineOrThrow(visitId, partLineId, currentUsername, "changed");

        // priceAtTimeOfUse holds the line total (unit price x quantity) from when the part was added.
        // Recover that original unit price so a later change of the part's price does not affect this line.
        BigDecimal unitPriceAtTimeOfUse = visitPart.getPriceAtTimeOfUse()
                .divide(BigDecimal.valueOf(visitPart.getQuantity()), 2, RoundingMode.HALF_UP);

        visitPart.setQuantity(dto.quantity());
        visitPart.setPriceAtTimeOfUse(unitPriceAtTimeOfUse.multiply(BigDecimal.valueOf(dto.quantity())));

        VisitPart updatedPart = visitPartRepository.save(visitPart);
        return visitPartMapper.toResponseDto(updatedPart);
    }

    @Override
    @Transactional
    public void deleteVisitPart(Long visitId, Long partLineId, String currentUsername) {

        VisitPart visitPart = getEditablePartLineOrThrow(visitId, partLineId, currentUsername, "removed");
        visitPartRepository.delete(visitPart);
    }

    // Shared rules for update and delete. The role (ADMIN or MECHANIC) is enforced in SecurityConfig;
    // here a MECHANIC must also be assigned to the visit, while an ADMIN always passes.
    private VisitPart getEditablePartLineOrThrow(Long visitId, Long partLineId, String currentUsername, String action) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (appUser.getRole() == Role.MECHANIC) {
            getAssignmentOrThrow(visitId, appUser.getId());
        }

        // Looking up by both IDs makes sure the part line really belongs to the visit in the URL.
        VisitPart visitPart = visitPartRepository.findByIdAndServiceVisitId(partLineId, visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Part line not found for this visit"));

        VisitStatus status = visitPart.getServiceVisit().getStatus();
        if (status == VisitStatus.COMPLETED || status == VisitStatus.INVOICED) {
            throw new InvalidStateTransitionException("Parts cannot be " + action + " after the visit is completed");
        }

        return visitPart;
    }

    private VisitAssignment getAssignmentOrThrow(Long visitId, Long mechanicId) {
        return visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, mechanicId)
                .orElseThrow(() -> new OwnershipViolationException("You are not assigned to this visit"));
    }
}
