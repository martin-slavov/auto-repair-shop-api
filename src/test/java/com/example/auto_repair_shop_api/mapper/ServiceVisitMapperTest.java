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
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceVisitMapperTest {

    private static final VisitViewer LEAD_VIEWER = new VisitViewer(Role.MECHANIC, 4L);
    private static final VisitViewer ASSISTANT_VIEWER = new VisitViewer(Role.MECHANIC, 5L);
    private static final VisitViewer OTHER_MECHANIC = new VisitViewer(Role.MECHANIC, 9L);
    private static final VisitViewer CUSTOMER = new VisitViewer(Role.CUSTOMER, 1L);

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

        lead = assignment(30L, 4L, "Ivan", "Petrov", AssignmentRole.LEAD);
        assistant = assignment(31L, 5L, "Georgi", "Ivanov", AssignmentRole.ASSISTANT);
    }

    @Test
    void adminGetsMechanicIdsAndAssignmentIdsButNoRole() {
        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, List.of(lead, assistant), VisitViewer.ADMIN);

        assertThat(result.mechanics()).containsExactly(
                new MechanicAssignmentDTO("Ivan Petrov", "LEAD", new BigDecimal("0.00"), 4L, 30L),
                new MechanicAssignmentDTO("Georgi Ivanov", "ASSISTANT", new BigDecimal("0.00"), 5L, 31L));
        assertThat(result.myRole()).isNull();
    }

    @Test
    void leadSeesAssignmentIdsAndTheirRoleButNoUserIds() {
        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, List.of(lead, assistant), LEAD_VIEWER);

        assertThat(result.myRole()).isEqualTo("LEAD");
        assertThat(result.mechanics()).extracting(MechanicAssignmentDTO::assignmentId).containsExactly(30L, 31L);
        assertThat(result.mechanics()).extracting(MechanicAssignmentDTO::mechanicId).containsOnlyNulls();
    }

    @Test
    void assistantSeesTheirRole() {
        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, List.of(lead, assistant), ASSISTANT_VIEWER);

        assertThat(result.myRole()).isEqualTo("ASSISTANT");
        assertThat(result.mechanics()).extracting(MechanicAssignmentDTO::assignmentId).containsExactly(30L, 31L);
    }

    @Test
    void customerAndUnassignedMechanicGetNoIdsAndNoRole() {
        for (VisitViewer viewer : List.of(CUSTOMER, OTHER_MECHANIC)) {
            ServiceVisitResponseDTO result = mapper.toResponseDto(visit, List.of(lead, assistant), viewer);

            assertThat(result.myRole()).isNull();
            assertThat(result.mechanics()).extracting(MechanicAssignmentDTO::mechanicId).containsOnlyNulls();
            assertThat(result.mechanics()).extracting(MechanicAssignmentDTO::assignmentId).containsOnlyNulls();
        }
    }

    @Test
    void includesTheRequestAndVehicleDetailsAfterTheExistingFields() {
        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, lead, CUSTOMER);

        assertThat(result).isEqualTo(new ServiceVisitResponseDTO(
                12L, "SCHEDULED", LocalDateTime.of(2026, 11, 23, 10, 0), null, null, "CA1234AB",
                List.of(new MechanicAssignmentDTO("Ivan Petrov", "LEAD", new BigDecimal("0.00"), null, null)),
                "INSPECTION", "Brakes squeak when stopping", "Toyota", "Corolla", null));
    }

    @Test
    void returnsNullRequestFieldsWhenTheVisitHasNoRequest() {
        visit.setServiceRequest(null);

        ServiceVisitResponseDTO result = mapper.toResponseDto(visit, lead, VisitViewer.ADMIN);

        assertThat(result.requestType()).isNull();
        assertThat(result.requestDescription()).isNull();
        assertThat(result.vehicleManufacturer()).isEqualTo("Toyota");
        assertThat(result.vehicleModel()).isEqualTo("Corolla");
        assertThat(result.licensePlate()).isEqualTo("CA1234AB");
    }

    private VisitAssignment assignment(Long id, Long mechanicId, String firstName, String lastName, AssignmentRole role) {
        AppUser mechanic = new AppUser();
        mechanic.setId(mechanicId);
        mechanic.setFirstName(firstName);
        mechanic.setLastName(lastName);
        mechanic.setRole(Role.MECHANIC);

        VisitAssignment assignment = new VisitAssignment();
        assignment.setId(id);
        assignment.setServiceVisit(visit);
        assignment.setMechanic(mechanic);
        assignment.setRoleInVisit(role);
        return assignment;
    }
}
