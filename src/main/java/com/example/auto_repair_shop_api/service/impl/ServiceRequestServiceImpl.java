package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.servicerequest.ApproveRequestDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestCreateDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestResponseDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.event.ServiceRequestCreatedEvent;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.ServiceRequestMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import com.example.auto_repair_shop_api.model.enums.RequestType;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.ServiceRequestRepository;
import com.example.auto_repair_shop_api.repository.VehicleRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.ServiceRequestService;
import com.example.auto_repair_shop_api.service.ServiceVisitService;
import com.example.auto_repair_shop_api.util.PageRequestFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.auto_repair_shop_api.repository.specification.ServiceRequestSpecifications.hasStatus;
import static com.example.auto_repair_shop_api.repository.specification.ServiceRequestSpecifications.hasType;
import static com.example.auto_repair_shop_api.repository.specification.ServiceRequestSpecifications.hasVehicleId;
import static com.example.auto_repair_shop_api.repository.specification.ServiceRequestSpecifications.licensePlateContains;
import static com.example.auto_repair_shop_api.repository.specification.ServiceRequestSpecifications.preferredOnOrAfter;
import static com.example.auto_repair_shop_api.repository.specification.ServiceRequestSpecifications.preferredOnOrBefore;
import static com.example.auto_repair_shop_api.repository.specification.ServiceRequestSpecifications.vehicleOwnedBy;

@Service
public class ServiceRequestServiceImpl implements ServiceRequestService {

    // Sort whitelist: API field name -> entity property.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "preferredDate", "preferredDate",
            "status", "status",
            "type", "type",
            "createdAt", "createdAt"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final AppUserService appUserService;
    private final ServiceVisitService serviceVisitService;
    private final ServiceRequestMapper serviceRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ServiceRequestServiceImpl(ServiceRequestRepository serviceRequestRepository,
                                     VehicleRepository vehicleRepository,
                                     AppUserService appUserService,
                                     ServiceVisitService serviceVisitService,
                                     ServiceRequestMapper serviceRequestMapper,
                                     ApplicationEventPublisher eventPublisher) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.vehicleRepository = vehicleRepository;
        this.appUserService = appUserService;
        this.serviceVisitService = serviceVisitService;
        this.serviceRequestMapper = serviceRequestMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ServiceRequestResponseDTO createRequest(ServiceRequestCreateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        Vehicle vehicle = vehicleRepository.findById(dto.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + dto.vehicleId()));

        if (!vehicle.getOwner().getId().equals(appUser.getId())) {
            throw new OwnershipViolationException("You can only create service requests for your own vehicles");
        }

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setDescription(dto.description());
        serviceRequest.setPreferredDate(dto.preferredDate());
        serviceRequest.setStatus(RequestStatus.PENDING);
        serviceRequest.setType(dto.type() != null ? dto.type() : RequestType.SERVICE);
        serviceRequest.setVehicle(vehicle);

        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);

        eventPublisher.publishEvent(new ServiceRequestCreatedEvent(vehicle.getLicensePlate(), dto.description()));

        return serviceRequestMapper.toResponseDto(savedRequest);
    }

    @Override
    public PageResponse<ServiceRequestResponseDTO> getRequestsForCurrentUser(String currentUsername,
                                                                             RequestStatus status, RequestType type,
                                                                             LocalDate preferredFrom, LocalDate preferredTo,
                                                                             String licensePlate, Long vehicleId,
                                                                             Integer page, Integer size, String sort) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (preferredFrom != null && preferredTo != null && preferredFrom.isAfter(preferredTo)) {
            throw new IllegalArgumentException("preferredFrom must not be after preferredTo");
        }

        PageRequest pageRequest = PageRequestFactory.create(page, size, sort, SORT_FIELDS, DEFAULT_SORT);

        List<Specification<ServiceRequest>> filters = new ArrayList<>();

        if (appUser.getRole() != Role.ADMIN) {
            filters.add(vehicleOwnedBy(appUser.getId()));
        }

        if (status != null) {
            filters.add(hasStatus(status));
        }
        if (type != null) {
            filters.add(hasType(type));
        }
        if (preferredFrom != null) {
            filters.add(preferredOnOrAfter(preferredFrom));
        }
        if (preferredTo != null) {
            filters.add(preferredOnOrBefore(preferredTo));
        }
        if (licensePlate != null && !licensePlate.isBlank()) {
            filters.add(licensePlateContains(licensePlate));
        }
        if (vehicleId != null) {
            filters.add(hasVehicleId(vehicleId));
        }

        Page<ServiceRequest> result = serviceRequestRepository.findAll(Specification.allOf(filters), pageRequest);
        return PageResponse.from(result, serviceRequestMapper::toResponseDto);
    }

    @Override
    @Transactional
    public ServiceRequestResponseDTO approveRequest(Long requestId, ApproveRequestDTO dto) {

        ServiceRequest serviceRequest = changeStatus(requestId, RequestStatus.APPROVED, "Only pending requests can be approved");
        serviceVisitService.createVisitFromRequest(serviceRequest, dto.mechanicId(), dto.scheduledDate());

        return serviceRequestMapper.toResponseDto(serviceRequest);
    }

    @Override
    @Transactional
    public ServiceRequestResponseDTO rejectRequest(Long requestId) {
        ServiceRequest result = changeStatus(requestId, RequestStatus.REJECTED, "Only pending requests can be rejected");
        return serviceRequestMapper.toResponseDto(result);
    }

    @Override
    @Transactional
    public ServiceRequestResponseDTO updateRequest(Long requestId, ServiceRequestUpdateDTO dto, String currentUsername) {

        ServiceRequest serviceRequest = getEditableRequestOrThrow(requestId, currentUsername, "updated");

        serviceRequest.setDescription(dto.description());
        serviceRequest.setPreferredDate(dto.preferredDate());
        serviceRequest.setType(dto.type());

        ServiceRequest updatedRequest = serviceRequestRepository.save(serviceRequest);
        return serviceRequestMapper.toResponseDto(updatedRequest);
    }

    @Override
    @Transactional
    public void deleteRequest(Long requestId, String currentUsername) {

        ServiceRequest serviceRequest = getEditableRequestOrThrow(requestId, currentUsername, "deleted");
        serviceRequestRepository.delete(serviceRequest);
    }

    private ServiceRequest getEditableRequestOrThrow(Long requestId, String currentUsername, String action) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);
        ServiceRequest serviceRequest = getRequestOrThrow(requestId);

        boolean isOwner = serviceRequest.getVehicle().getOwner().getId().equals(appUser.getId());
        boolean isAdmin = appUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new OwnershipViolationException("You can only modify service requests for your own vehicles");
        }

        if (serviceRequest.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Only pending requests can be " + action);
        }

        return serviceRequest;
    }

    private ServiceRequest getRequestOrThrow(Long requestId) {
        return serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));
    }

    private ServiceRequest changeStatus(Long requestId, RequestStatus status, String illegalStateMessage) {

        ServiceRequest serviceRequest = getRequestOrThrow(requestId);

        if (serviceRequest.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStateTransitionException(illegalStateMessage);
        }

        serviceRequest.setStatus(status);
        return serviceRequestRepository.save(serviceRequest);
    }
}
