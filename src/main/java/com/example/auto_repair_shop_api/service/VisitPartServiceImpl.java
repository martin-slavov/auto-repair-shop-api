package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Part;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.VisitPart;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.PartRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VisitPartServiceImpl implements VisitPartService {

    @Autowired
    private VisitPartRepository visitPartRepository;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private VisitAssignmentRepository visitAssignmentRepository;

    @Autowired
    private PartRepository partRepository;

    @Override
    @Transactional
    public VisitPartResponseDTO addPartToVisit(Long visitId, VisitPartCreateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        VisitAssignment assignment = visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, appUser.getId())
                .orElseThrow(() -> new SecurityException("You are not assigned to this visit"));

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

        return toResponseDto(savedPart);
    }

    @Override
    public List<VisitPartResponseDTO> getPartsForVisit(Long visitId, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (appUser.getRole() == Role.MECHANIC) {
            visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, appUser.getId())
                    .orElseThrow(() -> new SecurityException("You are not assigned to this visit"));
        }

        List<VisitPart> parts = visitPartRepository.findByServiceVisitId(visitId);

        return parts.stream()
                .map(this::toResponseDto)
                .toList();
    }

    private VisitPartResponseDTO toResponseDto(VisitPart visitPart) {
        return new VisitPartResponseDTO(
                visitPart.getId(),
                visitPart.getPart().getName(),
                visitPart.getQuantity(),
                visitPart.getPriceAtTimeOfUse()
        );
    }
}
