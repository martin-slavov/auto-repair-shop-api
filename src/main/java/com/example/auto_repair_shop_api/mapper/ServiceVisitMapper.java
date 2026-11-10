package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.servicevisit.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceVisitMapper {

    public ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, VisitAssignment assignment) {
        return toResponseDto(visit, List.of(assignment));
    }

    // Used when the caller is not one specific mechanic (e.g. an admin): shows every assigned mechanic.
    public ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, List<VisitAssignment> assignments) {

        List<MechanicAssignmentDTO> mechanics = assignments.stream()
                .map(assignment -> new MechanicAssignmentDTO(
                        assignment.getMechanic().getFirstName() + " " + assignment.getMechanic().getLastName(),
                        assignment.getRoleInVisit().name(),
                        assignment.getHoursWorked()
                ))
                .toList();

        return new ServiceVisitResponseDTO(
                visit.getId(), visit.getStatus().name(), visit.getScheduledDate(),
                visit.getCompletedDate(), visit.getTechnicalNotes(),
                visit.getVehicle().getLicensePlate(), mechanics
        );
    }
}
