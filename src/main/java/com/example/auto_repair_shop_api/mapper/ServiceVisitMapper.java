package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.servicevisit.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ServiceVisitMapper {

    public ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, VisitAssignment assignment, VisitViewer viewer) {
        return toResponseDto(visit, List.of(assignment), viewer);
    }

    public ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, List<VisitAssignment> assignments, VisitViewer viewer) {

        boolean isAdmin = viewer.role() == Role.ADMIN;
        String myRole = viewer.role() == Role.MECHANIC
                ? assignments.stream()
                        .filter(assignment -> Objects.equals(assignment.getMechanic().getId(), viewer.userId()))
                        .findFirst()
                        .map(assignment -> assignment.getRoleInVisit().name())
                        .orElse(null)
                : null;
        boolean showAssignmentIds = isAdmin || myRole != null;

        List<MechanicAssignmentDTO> mechanics = assignments.stream()
                .map(assignment -> new MechanicAssignmentDTO(
                        assignment.getMechanic().getFirstName() + " " + assignment.getMechanic().getLastName(),
                        assignment.getRoleInVisit().name(),
                        assignment.getHoursWorked(),
                        isAdmin ? assignment.getMechanic().getId() : null,
                        showAssignmentIds ? assignment.getId() : null
                ))
                .toList();

        ServiceRequest request = visit.getServiceRequest();
        Vehicle vehicle = visit.getVehicle();

        return new ServiceVisitResponseDTO(
                visit.getId(), visit.getStatus().name(), visit.getScheduledDate(),
                visit.getCompletedDate(), visit.getTechnicalNotes(),
                vehicle.getLicensePlate(), mechanics,
                request != null && request.getType() != null ? request.getType().name() : null,
                request != null ? request.getDescription() : null,
                vehicle.getManufacturer(),
                vehicle.getModel(),
                myRole
        );
    }
}
