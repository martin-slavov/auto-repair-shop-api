package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.ApproveRequestDTO;
import com.example.auto_repair_shop_api.dto.ServiceRequestCreateDTO;
import com.example.auto_repair_shop_api.dto.ServiceRequestResponseDTO;
import com.example.auto_repair_shop_api.mapper.ServiceRequestMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.ServiceRequestRepository;
import com.example.auto_repair_shop_api.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceRequestServiceImpl implements ServiceRequestService {


    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final AppUserService appUserService;
    private final ServiceVisitService serviceVisitService;
    private final ServiceRequestMapper serviceRequestMapper;

    @Autowired
    public ServiceRequestServiceImpl(ServiceRequestRepository serviceRequestRepository, VehicleRepository vehicleRepository, AppUserService appUserService, ServiceVisitService serviceVisitService, ServiceRequestMapper serviceRequestMapper) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.vehicleRepository = vehicleRepository;
        this.appUserService = appUserService;
        this.serviceVisitService = serviceVisitService;
        this.serviceRequestMapper = serviceRequestMapper;
    }

    @Override
    @Transactional
    public ServiceRequestResponseDTO createRequest(ServiceRequestCreateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + dto.getVehicleId()));

        if (!vehicle.getOwner().getId().equals(appUser.getId())) {
            throw new IllegalArgumentException("You can only create service requests for your own vehicles");
        }

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setDescription(dto.getDescription());
        serviceRequest.setStatus(RequestStatus.PENDING);
        serviceRequest.setVehicle(vehicle);

        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);

        return serviceRequestMapper.toResponseDto(savedRequest);
    }

    @Override
    public List<ServiceRequestResponseDTO> getRequestsForCurrentUser(String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        List<ServiceRequest> requests;
        if (appUser.getRole() == Role.ADMIN) {
            requests = serviceRequestRepository.findByStatus(RequestStatus.PENDING);
        } else {
            requests = serviceRequestRepository.findByVehicleOwnerId(appUser.getId());
        }

        return requests.stream()
                .map(serviceRequestMapper::toResponseDto)
                .toList();
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

    private ServiceRequest changeStatus(Long requestId, RequestStatus status, String illegalStateMessage) {

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Service request not found with ID: " + requestId));

        if (serviceRequest.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(illegalStateMessage);
        }

        serviceRequest.setStatus(status);
        return serviceRequestRepository.save(serviceRequest);
    }
}
