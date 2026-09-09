package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.ServiceVisitResponseDTO;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceVisitServiceImpl implements ServiceVisitService {

    @Autowired
    private ServiceVisitRepository serviceVisitRepository;

    @Autowired
    private VisitAssignmentRepository visitAssignmentRepository;

    @Autowired
    private AppUserService appUserService;

    @Override
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

        return toResponseDto(savedVisit, savedAssignment);
    }

    @Override
    public List<ServiceVisitResponseDTO> getVisitsForMechanic(String currentUsername) {

        AppUser mechanic = appUserService.getByUsernameOrThrow(currentUsername);

        List<VisitAssignment> assignments = visitAssignmentRepository.findByMechanicId(mechanic.getId());

        return assignments.stream()
                .map(assignment -> toResponseDto(assignment.getServiceVisit(), assignment))
                .toList();
    }

    @Override
    public ServiceVisitResponseDTO updateVisitStatus(Long visitId, VisitStatus newStatus, String currentUsername) {

        AppUser mechanic = appUserService.getByUsernameOrThrow(currentUsername);

        ServiceVisit serviceVisit = serviceVisitRepository.findById(visitId).orElseThrow(() -> new IllegalArgumentException("Service visit not found with ID: " + visitId));

        VisitAssignment assignment = visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, mechanic.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mechanic is not assigned to this service visit"));

        if (serviceVisit.getStatus() != VisitStatus.SCHEDULED || newStatus != VisitStatus.IN_PROGRESS) {
            throw new IllegalStateException("Invalid status transition");
        }

        serviceVisit.setStatus(newStatus);
        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return toResponseDto(updatedVisit, assignment);
    }

    @Override
    public ServiceVisitResponseDTO completeVisit(Long visitId, String technicalNotes, String currentUsername) {
        AppUser mechanic = appUserService.getByUsernameOrThrow(currentUsername);

        ServiceVisit serviceVisit = serviceVisitRepository.findById(visitId).orElseThrow(() -> new IllegalArgumentException("Service visit not found with ID: " + visitId));

        VisitAssignment assignment = visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, mechanic.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mechanic is not assigned to this service visit"));

        if (serviceVisit.getStatus() != VisitStatus.IN_PROGRESS || !assignment.getRoleInVisit().equals(AssignmentRole.LEAD)) {
            throw new IllegalStateException("Only the lead mechanic can complete a visit that is in progress");
        }

        serviceVisit.setStatus(VisitStatus.COMPLETED);
        serviceVisit.setTechnicalNotes(technicalNotes);
        serviceVisit.setCompletedDate(LocalDateTime.now());

        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return toResponseDto(updatedVisit, assignment);
    }

    private ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, VisitAssignment assignment) {
        MechanicAssignmentDTO mechanicDto = new MechanicAssignmentDTO(
                assignment.getMechanic().getFirstName() + " " + assignment.getMechanic().getLastName(),
                assignment.getRoleInVisit().name(),
                assignment.getHoursWorked()
        );
        return new ServiceVisitResponseDTO(
                visit.getId(), visit.getStatus().name(), visit.getScheduledDate(),
                visit.getCompletedDate(), visit.getTechnicalNotes(),
                visit.getVehicle().getLicensePlate(), List.of(mechanicDto)
        );
    }
}
