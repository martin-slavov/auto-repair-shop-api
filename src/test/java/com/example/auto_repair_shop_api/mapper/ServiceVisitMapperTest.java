package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.servicevisit.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceVisitMapperTest {

    private final ServiceVisitMapper mapper = new ServiceVisitMapper();

    private ServiceVisit visit;
    private VisitAssignment lead;
    private VisitAssignment assistant;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate("CA1234AB");

        visit = new ServiceVisit();
        visit.setId(12L);
        visit.setStatus(VisitStatus.SCHEDULED);
        visit.setScheduledDate(LocalDateTime.of(2026, 11, 23, 10, 0));
        visit.setVehicle(vehicle);

        lead = assignment(4L, "Ivan", "Petrov", AssignmentRole.LEAD);
        assistant = assignment(5L, "Georgi", "Ivanov", AssignmentRole.ASSISTANT);
    }

    @Test
    void includesTheMechanicIdsWhenAsked() {
        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, List.of(lead, assistant), true);

        assertThat(result.mechanics()).containsExactly(
                new MechanicAssignmentDTO("Ivan Petrov", "LEAD", new BigDecimal("0.00"), 4L),
                new MechanicAssignmentDTO("Georgi Ivanov", "ASSISTANT", new BigDecimal("0.00"), 5L));
    }

    @Test
    void leavesTheMechanicIdNullOtherwiseAndKeepsTheOtherFields() {
        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, lead, false);

        assertThat(result.mechanics()).containsExactly(
                new MechanicAssignmentDTO("Ivan Petrov", "LEAD", new BigDecimal("0.00"), null));
        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.licensePlate()).isEqualTo("CA1234AB");
    }

    private VisitAssignment assignment(Long mechanicId, String firstName, String lastName, AssignmentRole role) {
        AppUser mechanic = new AppUser();
        mechanic.setId(mechanicId);
        mechanic.setFirstName(firstName);
        mechanic.setLastName(lastName);

        VisitAssignment assignment = new VisitAssignment();
        assignment.setServiceVisit(visit);
        assignment.setMechanic(mechanic);
        assignment.setRoleInVisit(role);
        return assignment;
    }
}
