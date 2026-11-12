package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
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
import com.example.auto_repair_shop_api.util.PageRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.assignedToMechanic;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.hasStatus;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.hasVehicleId;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.licensePlateContains;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.scheduledOnOrAfter;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.scheduledOnOrBefore;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.vehicleOwnedBy;

@Service
public class ServiceVisitServiceImpl implements ServiceVisitService {

    // Sort whitelist: API field name -> entity property.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "scheduledDate", "scheduledDate",
            "completedDate", "completedDate",
            "status", "status"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "scheduledDate");

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
    public PageResponse<ServiceVisitResponseDTO> getVisitsForCurrentUser(String currentUsername,
                                                                         VisitStatus status,
                                                                         LocalDate scheduledFrom, LocalDate scheduledTo,
                                                                         String licensePlate, Long vehicleId, Long mechanicId,
                                                                         Integer page, Integer size, String sort) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (scheduledFrom != null && scheduledTo != null && scheduledFrom.isAfter(scheduledTo)) {
            throw new IllegalArgumentException("scheduledFrom must not be after scheduledTo");
        }

        PageRequest pageRequest = PageRequestFactory.create(page, size, sort, SORT_FIELDS, DEFAULT_SORT);

        List<Specification<ServiceVisit>> filters = new ArrayList<>();

        switch (appUser.getRole()) {
            case ADMIN -> { }
            case MECHANIC -> filters.add(assignedToMechanic(appUser.getId()));
            case CUSTOMER -> filters.add(vehicleOwnedBy(appUser.getId()));
            default -> throw new IllegalStateException("Unsupported role: " + appUser.getRole());
        }

        if (status != null) {
            filters.add(hasStatus(status));
        }
        if (scheduledFrom != null) {
            filters.add(scheduledOnOrAfter(scheduledFrom));
        }
        if (scheduledTo != null) {
            filters.add(scheduledOnOrBefore(scheduledTo));
        }
        if (licensePlate != null && !licensePlate.isBlank()) {
            filters.add(licensePlateContains(licensePlate));
        }
        if (vehicleId != null) {
            filters.add(hasVehicleId(vehicleId));
        }
        // Filtering by mechanic is an admin feature; ignored for other roles.
        if (mechanicId != null && appUser.getRole() == Role.ADMIN) {
            filters.add(assignedToMechanic(mechanicId));
        }

        Page<ServiceVisit> result = serviceVisitRepository.findAll(Specification.allOf(filters), pageRequest);

        Map<Long, List<VisitAssignment>> assignmentsByVisitId = result.hasContent()
                ? visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(
                                result.getContent().stream().map(ServiceVisit::getId).toList()).stream()
                        .collect(Collectors.groupingBy(assignment -> assignment.getServiceVisit().getId()))
                : Map.of();

        return PageResponse.from(result, visit ->
                serviceVisitMapper.toResponseDto(visit, assignmentsByVisitId.getOrDefault(visit.getId(), List.of())));
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
