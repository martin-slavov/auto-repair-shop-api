package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.servicevisit.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceVisitMapper {

    public ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, VisitAssignment assignment, boolean includeMechanicId) {
        return toResponseDto(visit, List.of(assignment), includeMechanicId);
    }

    // Used when the caller is not one specific mechanic (e.g. an admin): shows every assigned mechanic.
    public ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, List<VisitAssignment> assignments, boolean includeMechanicId) {

        List<MechanicAssignmentDTO> mechanics = assignments.stream()
                .map(assignment -> new MechanicAssignmentDTO(
                        assignment.getMechanic().getFirstName() + " " + assignment.getMechanic().getLastName(),
                        assignment.getRoleInVisit().name(),
                        assignment.getHoursWorked(),
                        includeMechanicId ? assignment.getMechanic().getId() : null
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
                vehicle.getModel()
        );
    }
}
