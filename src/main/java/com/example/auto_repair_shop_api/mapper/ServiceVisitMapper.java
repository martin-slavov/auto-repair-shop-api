package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceVisitMapper {

    public ServiceVisitResponseDTO toResponseDto(ServiceVisit visit, VisitAssignment assignment) {

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
