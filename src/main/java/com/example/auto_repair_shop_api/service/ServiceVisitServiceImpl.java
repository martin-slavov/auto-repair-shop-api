package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.mapper.ServiceVisitMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceVisitServiceImpl implements ServiceVisitService {

    private final ServiceVisitRepository serviceVisitRepository;
    private final VisitAssignmentRepository visitAssignmentRepository;
    private final AppUserService appUserService;
    private final ServiceVisitMapper serviceVisitMapper;

    @Autowired
    public ServiceVisitServiceImpl(ServiceVisitRepository serviceVisitRepository, VisitAssignmentRepository visitAssignmentRepository, AppUserService appUserService, ServiceVisitMapper serviceVisitMapper) {
        this.serviceVisitRepository = serviceVisitRepository;
        this.visitAssignmentRepository = visitAssignmentRepository;
        this.appUserService = appUserService;
        this.serviceVisitMapper = serviceVisitMapper;
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
        assignment.setHoursWorked(0.0);

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
            throw new IllegalStateException("Invalid status transition");
        }

        serviceVisit.setStatus(newStatus);
        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, assignment);
    }

    @Override
    @Transactional
    public ServiceVisitResponseDTO completeVisit(Long visitId, String technicalNotes, double hoursWorked, String currentUsername) {

        VisitAssignment assignment = getMechanicAssignmentOrThrow(visitId, currentUsername);
        ServiceVisit serviceVisit = assignment.getServiceVisit();

        if (serviceVisit.getStatus() != VisitStatus.IN_PROGRESS || !assignment.getRoleInVisit().equals(AssignmentRole.LEAD)) {
            throw new IllegalStateException("Only the lead mechanic can complete a visit that is in progress");
        }

        serviceVisit.setStatus(VisitStatus.COMPLETED);
        serviceVisit.setTechnicalNotes(technicalNotes);
        serviceVisit.setCompletedDate(LocalDateTime.now());

        assignment.setHoursWorked(hoursWorked);
        visitAssignmentRepository.save(assignment);

        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, assignment);
    }

    private VisitAssignment getMechanicAssignmentOrThrow(Long visitId, String currentUsername) {
        AppUser mechanic = appUserService.getByUsernameOrThrow(currentUsername);
        return visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, mechanic.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mechanic is not assigned to this service visit"));
    }
}
