package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitUpdateDTO;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.ServiceVisitMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.ServiceVisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceVisitServiceImpl implements ServiceVisitService {

    private final ServiceVisitRepository serviceVisitRepository;
    private final VisitAssignmentRepository visitAssignmentRepository;
    private final AppUserService appUserService;
    private final ServiceVisitMapper serviceVisitMapper;
    private final VisitPartRepository visitPartRepository;

    @Autowired
    public ServiceVisitServiceImpl(ServiceVisitRepository serviceVisitRepository, VisitAssignmentRepository visitAssignmentRepository, AppUserService appUserService, ServiceVisitMapper serviceVisitMapper,
                                   VisitPartRepository visitPartRepository) {
        this.serviceVisitRepository = serviceVisitRepository;
        this.visitAssignmentRepository = visitAssignmentRepository;
        this.appUserService = appUserService;
        this.serviceVisitMapper = serviceVisitMapper;
        this.visitPartRepository = visitPartRepository;
    }

    @Override
    @Transactional
    public ServiceVisitResponseDTO createVisitFromRequest(ServiceRequest serviceRequest, Long mechanicId, LocalDateTime scheduledDate) {

        AppUser mechanic = appUserService.getByIdOrThrow(mechanicId);

        if (mechanic.getRole() != Role.MECHANIC) {
            throw new IllegalArgumentException("User with ID " + mechanicId + " is not a mechanic");
        }

        ServiceVisit serviceVisit = new ServiceVisit();
        serviceVisit.setStatus(VisitStatus.SCHEDULED);
        serviceVisit.setScheduledDate(scheduledDate);
        serviceVisit.setServiceRequest(serviceRequest);
        serviceVisit.setVehicle(serviceRequest.getVehicle());

        ServiceVisit savedVisit = serviceVisitRepository.save(serviceVisit);

        VisitAssignment assignment = new VisitAssignment();
        assignment.setServiceVisit(savedVisit);
        assignment.setMechanic(mechanic);
        assignment.setRoleInVisit(AssignmentRole.LEAD);

        VisitAssignment savedAssignment = visitAssignmentRepository.save(assignment);

        return serviceVisitMapper.toResponseDto(savedVisit, savedAssignment);
    }

    @Override
    public List<ServiceVisitResponseDTO> getVisitsForMechanic(String currentUsername) {

        AppUser mechanic = appUserService.getByUsernameOrThrow(currentUsername);

        List<VisitAssignment> assignments = visitAssignmentRepository.findByMechanicId(mechanic.getId());

        return assignments.stream()
                .map(assignment -> serviceVisitMapper.toResponseDto(assignment.getServiceVisit(), assignment))
                .toList();
    }

    @Override
    @Transactional
    public ServiceVisitResponseDTO updateVisitStatus(Long visitId, VisitStatus newStatus, String currentUsername) {

        VisitAssignment assignment = getMechanicAssignmentOrThrow(visitId, currentUsername);
        ServiceVisit serviceVisit = assignment.getServiceVisit();

        if (serviceVisit.getStatus() != VisitStatus.SCHEDULED || newStatus != VisitStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Invalid status transition");
        }

        serviceVisit.setStatus(newStatus);
        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, assignment);
    }

    @Override
    @Transactional
    public ServiceVisitResponseDTO completeVisit(Long visitId, String technicalNotes, BigDecimal hoursWorked, String currentUsername) {

        VisitAssignment assignment = getMechanicAssignmentOrThrow(visitId, currentUsername);
        ServiceVisit serviceVisit = assignment.getServiceVisit();

        if (serviceVisit.getStatus() != VisitStatus.IN_PROGRESS || !assignment.getRoleInVisit().equals(AssignmentRole.LEAD)) {
            throw new InvalidStateTransitionException("Only the lead mechanic can complete a visit that is in progress");
        }

        serviceVisit.setStatus(VisitStatus.COMPLETED);
        serviceVisit.setTechnicalNotes(technicalNotes);
        serviceVisit.setCompletedDate(LocalDateTime.now());

        assignment.setHoursWorked(hoursWorked);
        visitAssignmentRepository.save(assignment);

        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, assignment);
    }

    // ADMIN only: enforced in SecurityConfig.
    @Override
    @Transactional
    public ServiceVisitResponseDTO updateVisit(Long visitId, ServiceVisitUpdateDTO dto) {

        ServiceVisit serviceVisit = getScheduledVisitOrThrow(visitId, "rescheduled");

        serviceVisit.setScheduledDate(dto.scheduledDate());
        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, visitAssignmentRepository.findByServiceVisitId(visitId));
    }

    // ADMIN only: enforced in SecurityConfig.
    @Override
    @Transactional
    public void deleteVisit(Long visitId) {

        ServiceVisit serviceVisit = getScheduledVisitOrThrow(visitId, "deleted");

        if (visitPartRepository.existsByServiceVisitId(visitId)) {
            throw new ResourceInUseException("Service visit cannot be deleted because it has parts");
        }

        // The request goes back to PENDING so it can be approved again (or edited/deleted by the customer).
        ServiceRequest serviceRequest = serviceVisit.getServiceRequest();
        serviceRequest.setStatus(RequestStatus.PENDING);

        // Assignment rows reference the visit through a foreign key, so they must be removed first.
        visitAssignmentRepository.deleteAll(visitAssignmentRepository.findByServiceVisitId(visitId));
        serviceVisitRepository.delete(serviceVisit);
    }

    private ServiceVisit getScheduledVisitOrThrow(Long visitId, String action) {
        ServiceVisit serviceVisit = serviceVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Service visit not found with ID: " + visitId));

        if (serviceVisit.getStatus() != VisitStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Only scheduled visits can be " + action);
        }

        return serviceVisit;
    }

    private VisitAssignment getMechanicAssignmentOrThrow(Long visitId, String currentUsername) {
        AppUser mechanic = appUserService.getByUsernameOrThrow(currentUsername);
        return visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, mechanic.getId())
                .orElseThrow(() -> new OwnershipViolationException("Mechanic is not assigned to this service visit"));
    }
}
