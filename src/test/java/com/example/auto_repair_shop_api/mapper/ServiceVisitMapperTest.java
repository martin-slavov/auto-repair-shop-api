package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.servicevisit.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import com.example.auto_repair_shop_api.model.enums.RequestType;
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
        vehicle.setManufacturer("Toyota");
        vehicle.setModel("Corolla");

        ServiceRequest request = new ServiceRequest();
        request.setType(RequestType.INSPECTION);
        request.setDescription("Brakes squeak when stopping");
        request.setVehicle(vehicle);

        visit = new ServiceVisit();
        visit.setId(12L);
        visit.setStatus(VisitStatus.SCHEDULED);
        visit.setScheduledDate(LocalDateTime.of(2026, 11, 23, 10, 0));
        visit.setVehicle(vehicle);
        visit.setServiceRequest(request);

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

    @Test
    void includesTheRequestAndVehicleDetailsAfterTheExistingFields() {
        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, lead, false);

        assertThat(result).isEqualTo(new ServiceVisitResponseDTO(
                12L, "SCHEDULED", LocalDateTime.of(2026, 11, 23, 10, 0), null, null, "CA1234AB",
                List.of(new MechanicAssignmentDTO("Ivan Petrov", "LEAD", new BigDecimal("0.00"), null)),
                "INSPECTION", "Brakes squeak when stopping", "Toyota", "Corolla"));
    }

    @Test
    void returnsNullRequestFieldsWhenTheVisitHasNoRequest() {
        visit.setServiceRequest(null);

        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, lead, true);

        assertThat(result.requestType()).isNull();
        assertThat(result.requestDescription()).isNull();
        assertThat(result.vehicleManufacturer()).isEqualTo("Toyota");
        assertThat(result.vehicleModel()).isEqualTo("Corolla");
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
