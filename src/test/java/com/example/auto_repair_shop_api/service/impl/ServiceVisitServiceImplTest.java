package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.config.BookingProperties;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.servicevisit.AssistantHoursDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.AvailableSlotDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.MechanicAssignmentDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitUpdateDTO;
import com.example.auto_repair_shop_api.exception.BookingConflictException;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.ServiceVisitMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import com.example.auto_repair_shop_api.model.enums.RequestType;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceVisitServiceImplTest {

    private static final long MECHANIC_ID = 4L;
    private static final long VISIT_ID = 12L;
    private static final long OTHER_VISIT_ID = 99L;
    private static final long ASSISTANT_ID = 5L;
    private static final long LEAD_ASSIGNMENT_ID = 30L;
    private static final long ASSISTANT_ASSIGNMENT_ID = 31L;

    // "Now" is Thursday 2026-11-19 10:10. Monday 2026-11-23 is a normal future working day.
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T10:10:00Z"), ZoneOffset.UTC);

    private static final List<VisitStatus> ACTIVE = List.of(VisitStatus.SCHEDULED, VisitStatus.IN_PROGRESS);

    @Mock
    private ServiceVisitRepository serviceVisitRepository;
    @Mock
    private VisitAssignmentRepository visitAssignmentRepository;
    @Mock
    private AppUserService appUserService;
    @Mock
    private VisitPartRepository visitPartRepository;

    private ServiceVisitServiceImpl serviceVisitService;

    private AppUser mechanic;
    private AppUser assistantUser;
    private Vehicle vehicle;
    private ServiceRequest serviceRequest;

    @BeforeEach
    void setUp() {
        serviceVisitService = new ServiceVisitServiceImpl(serviceVisitRepository, visitAssignmentRepository,
                appUserService, new ServiceVisitMapper(), visitPartRepository, new BookingProperties(), CLOCK);

        mechanic = new AppUser();
        mechanic.setId(MECHANIC_ID);
        mechanic.setUsername("mech");
        mechanic.setFirstName("Ivan");
        mechanic.setLastName("Petrov");
        mechanic.setRole(Role.MECHANIC);
        mechanic.setEnabled(true);

        assistantUser = user(ASSISTANT_ID, "helper", Role.MECHANIC);
        assistantUser.setFirstName("Georgi");
        assistantUser.setLastName("Ivanov");

        vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setLicensePlate("CA1234AB");
        vehicle.setManufacturer("Toyota");
        vehicle.setModel("Corolla");

        serviceRequest = new ServiceRequest();
        serviceRequest.setId(3L);
        serviceRequest.setType(RequestType.SERVICE);
        serviceRequest.setDescription("Brakes squeak when stopping");
        serviceRequest.setVehicle(vehicle);
    }

    // ---- approve (createVisitFromRequest) ----

    @Test
    void approveOnAFreeSlotCreatesTheVisitAndChecksTheOverlapWindow() {
        LocalDateTime start = MONDAY.atTime(10, 30);
        stubMechanic();
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(
                List.of(MECHANIC_ID), ACTIVE, MONDAY.atTime(9, 30), MONDAY.atTime(11, 30)))
                .thenReturn(List.of());
        when(serviceVisitRepository.save(any(ServiceVisit.class))).thenAnswer(call -> {
            ServiceVisit saved = call.getArgument(0);
            saved.setId(VISIT_ID);
            return saved;
        });
        when(visitAssignmentRepository.save(any(VisitAssignment.class))).thenAnswer(call -> call.getArgument(0));

        ServiceVisitResponseDTO result = serviceVisitService.createVisitFromRequest(serviceRequest, MECHANIC_ID, start);

        assertThat(result.id()).isEqualTo(VISIT_ID);
        assertThat(result.status()).isEqualTo("SCHEDULED");
        assertThat(result.scheduledDate()).isEqualTo(start);
        assertWorkDetails(result);
        assertThat(result.mechanics()).singleElement().satisfies(m -> {
            assertThat(m.roleInVisit()).isEqualTo("LEAD");
            assertThat(m.mechanicId()).isEqualTo(MECHANIC_ID);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"10:15", "10:45", "09:01", "10:30:30"})
    void approveRejectsAStartThatIsNotOnASlot(String time) {
        stubMechanic();

        assertThatThrownBy(() -> serviceVisitService.createVisitFromRequest(
                serviceRequest, MECHANIC_ID, MONDAY.atTime(LocalTime.parse(time))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled time must start on a 30-minute slot (e.g. 09:00, 09:30)");

        verifyNothingBookedOrSaved();
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:30", "16:30", "17:00", "00:00", "20:00"})
    void approveRejectsAStartOutsideWorkingHours(String time) {
        stubMechanic();

        assertThatThrownBy(() -> serviceVisitService.createVisitFromRequest(
                serviceRequest, MECHANIC_ID, MONDAY.atTime(LocalTime.parse(time))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled time must be between 09:00 and 16:00 "
                        + "(working hours 09:00-17:00, a visit takes 60 minutes)");

        verifyNothingBookedOrSaved();
    }

    @Test
    void approveAcceptsTheFirstAndTheLastSlotOfTheDay() {
        stubMechanic();
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(anyCollection(), anyCollection(), any(), any()))
                .thenReturn(List.of());
        when(serviceVisitRepository.save(any(ServiceVisit.class))).thenAnswer(call -> call.getArgument(0));
        when(visitAssignmentRepository.save(any(VisitAssignment.class))).thenAnswer(call -> call.getArgument(0));

        serviceVisitService.createVisitFromRequest(serviceRequest, MECHANIC_ID, MONDAY.atTime(9, 0));
        serviceVisitService.createVisitFromRequest(serviceRequest, MECHANIC_ID, MONDAY.atTime(16, 0));

        verify(serviceVisitRepository, times(2)).save(any(ServiceVisit.class));
    }

    @Test
    void approveRejectsAWeekend() {
        stubMechanic();
        LocalDateTime saturday = LocalDate.of(2026, 11, 21).atTime(10, 0);

        assertThatThrownBy(() -> serviceVisitService.createVisitFromRequest(serviceRequest, MECHANIC_ID, saturday))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled date must be on a working day (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)");

        verifyNothingBookedOrSaved();
    }

    @Test
    void approveRejectsAnOverlappingVisitOfTheMechanic() {
        stubMechanic();
        // Existing visit 10:30-11:30, new one 11:00-12:00.
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(
                List.of(MECHANIC_ID), ACTIVE, MONDAY.atTime(10, 0), MONDAY.atTime(12, 0)))
                .thenReturn(List.of(visit(OTHER_VISIT_ID, MONDAY.atTime(10, 30))));

        assertThatThrownBy(() -> serviceVisitService.createVisitFromRequest(
                serviceRequest, MECHANIC_ID, MONDAY.atTime(11, 0)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Mechanic is already booked at this time");

        verify(serviceVisitRepository, never()).save(any());
        verify(visitAssignmentRepository, never()).save(any());
    }

    @Test
    void approveRejectsADisabledMechanicBeforeLookingAtTheSchedule() {
        mechanic.setEnabled(false);
        stubMechanic();

        assertThatThrownBy(() -> serviceVisitService.createVisitFromRequest(
                serviceRequest, MECHANIC_ID, MONDAY.atTime(10, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mechanic account is disabled");

        verifyNothingBookedOrSaved();
    }

    // ---- reschedule (updateVisit) ----

    @Test
    void rescheduleRejectsAnOverlapWithAnotherVisitOfAnAssignedMechanic() {
        ServiceVisit visit = stubScheduledVisitWithLead(MONDAY.atTime(9, 0));
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(
                List.of(MECHANIC_ID), ACTIVE, MONDAY.atTime(13, 30), MONDAY.atTime(15, 30)))
                .thenReturn(List.of(visit(OTHER_VISIT_ID, MONDAY.atTime(15, 0))));

        assertThatThrownBy(() -> serviceVisitService.updateVisit(VISIT_ID,
                new ServiceVisitUpdateDTO(MONDAY.atTime(14, 30))))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Mechanic is already booked at this time");

        assertThat(visit.getScheduledDate()).isEqualTo(MONDAY.atTime(9, 0));
        verify(serviceVisitRepository, never()).save(any());
    }

    @Test
    void rescheduleOntoItsOwnSlotIsAllowed() {
        ServiceVisit visit = stubScheduledVisitWithLead(MONDAY.atTime(10, 0));
        // The only visit found in the window is the one being rescheduled.
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(anyCollection(), anyCollection(), any(), any()))
                .thenReturn(List.of(visit));
        when(serviceVisitRepository.save(visit)).thenReturn(visit);

        ServiceVisitResponseDTO result = serviceVisitService.updateVisit(VISIT_ID,
                new ServiceVisitUpdateDTO(MONDAY.atTime(10, 30)));

        assertThat(result.scheduledDate()).isEqualTo(MONDAY.atTime(10, 30));
        assertWorkDetails(result);
        assertThat(result.mechanics()).singleElement()
                .satisfies(m -> assertThat(m.mechanicId()).isEqualTo(MECHANIC_ID));
    }

    @Test
    void rescheduleRejectsAStartOutsideWorkingHours() {
        stubScheduledVisitWithLead(MONDAY.atTime(10, 0));

        assertThatThrownBy(() -> serviceVisitService.updateVisit(VISIT_ID,
                new ServiceVisitUpdateDTO(MONDAY.atTime(16, 30))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Scheduled time must be between 09:00 and 16:00");

        verify(serviceVisitRepository, never()).findByMechanicsAndStatusStartingBetween(any(), any(), any(), any());
        verify(serviceVisitRepository, never()).save(any());
    }

    // ---- available slots ----

    @Test
    void availableSlotsForADayWithOneBookedVisitUseOneQuery() {
        stubMechanic();
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(
                List.of(MECHANIC_ID), ACTIVE, MONDAY.atTime(8, 0), MONDAY.atTime(17, 0)))
                .thenReturn(List.of(visit(OTHER_VISIT_ID, MONDAY.atTime(11, 0))));

        List<AvailableSlotDTO> slots = serviceVisitService.getAvailableSlots(MECHANIC_ID, MONDAY, null);

        assertThat(slots).hasSize(15);
        assertThat(slots.getFirst().start()).isEqualTo(MONDAY.atTime(9, 0));
        assertThat(slots.getLast().start()).isEqualTo(MONDAY.atTime(16, 0));
        // A 60-minute visit at 11:00 blocks starts at 10:30, 11:00 and 11:30; 10:00 and 12:00 are back to back.
        assertThat(unavailableTimes(slots)).containsExactly(
                LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(11, 30));
        verify(serviceVisitRepository, times(1))
                .findByMechanicsAndStatusStartingBetween(any(), any(), any(), any());
    }

    @Test
    void availableSlotsTreatTheExcludedVisitAsFree() {
        stubMechanic();
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(anyCollection(), anyCollection(), any(), any()))
                .thenReturn(List.of(visit(VISIT_ID, MONDAY.atTime(11, 0)), visit(OTHER_VISIT_ID, MONDAY.atTime(14, 0))));

        List<AvailableSlotDTO> slots = serviceVisitService.getAvailableSlots(MECHANIC_ID, MONDAY, VISIT_ID);

        assertThat(unavailableTimes(slots)).containsExactly(
                LocalTime.of(13, 30), LocalTime.of(14, 0), LocalTime.of(14, 30));
    }

    @Test
    void pastSlotsOfTodayAreUnavailable() {
        stubMechanic();
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(anyCollection(), anyCollection(), any(), any()))
                .thenReturn(List.of());

        List<AvailableSlotDTO> slots = serviceVisitService.getAvailableSlots(MECHANIC_ID, TODAY, null);

        // Now is 10:10.
        assertThat(unavailableTimes(slots)).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0));
    }

    @Test
    void aDayInThePastIsFullyUnavailableWithoutAQuery() {
        stubMechanic();

        List<AvailableSlotDTO> slots = serviceVisitService.getAvailableSlots(MECHANIC_ID, TODAY.minusDays(1), null);

        assertThat(slots).hasSize(15).noneMatch(AvailableSlotDTO::available);
        verifyNoInteractions(serviceVisitRepository);
    }

    @Test
    void aWeekendIsFullyUnavailableWithoutAQuery() {
        stubMechanic();

        List<AvailableSlotDTO> slots = serviceVisitService.getAvailableSlots(
                MECHANIC_ID, LocalDate.of(2026, 11, 22), null);

        assertThat(slots).hasSize(15).noneMatch(AvailableSlotDTO::available);
        verifyNoInteractions(serviceVisitRepository);
    }

    @Test
    void availableSlotsRejectAUserWhoIsNotAMechanic() {
        mechanic.setRole(Role.CUSTOMER);
        stubMechanic();

        assertThatThrownBy(() -> serviceVisitService.getAvailableSlots(MECHANIC_ID, MONDAY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with ID 4 is not a mechanic");

        verifyNoInteractions(serviceVisitRepository);
    }

    // ---- mechanicId visibility ----

    @Test
    void adminSeesTheMechanicIdInTheVisitList() {
        AppUser admin = user(1L, "admin", Role.ADMIN);
        stubVisitList(admin, visit(VISIT_ID, MONDAY.atTime(10, 0)));

        PageResponse<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForCurrentUser(
                "admin", null, null, null, null, null, null, null, null, null);

        assertThat(result.content()).singleElement().satisfies(dto -> {
            assertWorkDetails(dto);
            assertThat(dto.myRole()).isNull();
            assertThat(dto.mechanics()).singleElement()
                    .satisfies(m -> assertThat(m.mechanicId()).isEqualTo(MECHANIC_ID));
        });
    }

    @Test
    void aVisitWithoutARequestIsListedWithNullRequestFields() {
        AppUser admin = user(1L, "admin", Role.ADMIN);
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        visit.setServiceRequest(null);
        stubVisitList(admin, visit);

        PageResponse<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForCurrentUser(
                "admin", null, null, null, null, null, null, null, null, null);

        assertThat(result.content()).singleElement().satisfies(dto -> {
            assertThat(dto.requestType()).isNull();
            assertThat(dto.requestDescription()).isNull();
            assertThat(dto.vehicleManufacturer()).isEqualTo("Toyota");
            assertThat(dto.vehicleModel()).isEqualTo("Corolla");
        });
    }

    @Test
    void customerGetsNoMechanicIdInTheVisitList() {
        AppUser customer = user(2L, "owner", Role.CUSTOMER);
        stubVisitList(customer, visit(VISIT_ID, MONDAY.atTime(10, 0)));

        PageResponse<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForCurrentUser(
                "owner", null, null, null, null, null, null, null, null, null);

        assertThat(result.content()).singleElement().satisfies(dto -> {
            assertWorkDetails(dto);
            assertThat(dto.myRole()).isNull();
            assertThat(dto.mechanics()).singleElement().satisfies(m -> {
                assertThat(m.mechanicName()).isEqualTo("Ivan Petrov");
                assertThat(m.mechanicId()).isNull();
                assertThat(m.assignmentId()).isNull();
            });
        });
    }

    @Test
    void mechanicGetsNoMechanicIdWhenStartingAVisit() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        when(appUserService.getByUsernameOrThrow("mech")).thenReturn(mechanic);
        when(visitAssignmentRepository.findByServiceVisitIdAndMechanicId(VISIT_ID, MECHANIC_ID))
                .thenReturn(Optional.of(lead(visit)));
        when(serviceVisitRepository.save(visit)).thenReturn(visit);

        ServiceVisitResponseDTO result = serviceVisitService.updateVisitStatus(VISIT_ID, VisitStatus.IN_PROGRESS, "mech");

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertWorkDetails(result);
        assertThat(result.mechanics()).singleElement().satisfies(m -> assertThat(m.mechanicId()).isNull());
    }

    // ---- assistants: add ----

    @Test
    void addAssistantSavesAnAssistantAndReturnsEveryAssignment() {
        ServiceVisit visit = stubVisit(VisitStatus.SCHEDULED);
        when(appUserService.getByIdOrThrow(ASSISTANT_ID)).thenReturn(assistantUser);
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID)))
                .thenReturn(List.of(assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD)));
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(
                List.of(ASSISTANT_ID), ACTIVE, MONDAY.atTime(9, 0), MONDAY.atTime(11, 0)))
                .thenReturn(List.of(visit));
        when(visitAssignmentRepository.save(any(VisitAssignment.class))).thenAnswer(call -> {
            VisitAssignment saved = call.getArgument(0);
            saved.setId(ASSISTANT_ASSIGNMENT_ID);
            return saved;
        });

        ServiceVisitResponseDTO result = serviceVisitService.addAssistant(VISIT_ID, ASSISTANT_ID);

        assertThat(result.mechanics()).containsExactly(
                new MechanicAssignmentDTO("Ivan Petrov", "LEAD", new BigDecimal("0.00"), MECHANIC_ID, LEAD_ASSIGNMENT_ID),
                new MechanicAssignmentDTO("Georgi Ivanov", "ASSISTANT", new BigDecimal("0.00"), ASSISTANT_ID, ASSISTANT_ASSIGNMENT_ID));
        assertThat(result.myRole()).isNull();
    }

    @Test
    void addAssistantRejectsAMechanicAlreadyOnTheVisit() {
        ServiceVisit visit = stubVisit(VisitStatus.IN_PROGRESS);
        when(appUserService.getByIdOrThrow(ASSISTANT_ID)).thenReturn(assistantUser);
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID))).thenReturn(List.of(
                assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD),
                assignment(ASSISTANT_ASSIGNMENT_ID, visit, assistantUser, AssignmentRole.ASSISTANT)));

        assertThatThrownBy(() -> serviceVisitService.addAssistant(VISIT_ID, ASSISTANT_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Mechanic is already assigned to this visit");

        verify(visitAssignmentRepository, never()).save(any());
    }

    @Test
    void addAssistantRejectsAUserWhoIsNotAMechanic() {
        stubVisit(VisitStatus.SCHEDULED);
        assistantUser.setRole(Role.CUSTOMER);
        when(appUserService.getByIdOrThrow(ASSISTANT_ID)).thenReturn(assistantUser);

        assertThatThrownBy(() -> serviceVisitService.addAssistant(VISIT_ID, ASSISTANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with ID 5 is not a mechanic");

        verifyNoInteractions(visitAssignmentRepository);
    }

    @Test
    void addAssistantRejectsADisabledMechanic() {
        stubVisit(VisitStatus.SCHEDULED);
        assistantUser.setEnabled(false);
        when(appUserService.getByIdOrThrow(ASSISTANT_ID)).thenReturn(assistantUser);

        assertThatThrownBy(() -> serviceVisitService.addAssistant(VISIT_ID, ASSISTANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mechanic account is disabled");

        verifyNoInteractions(visitAssignmentRepository);
    }

    @Test
    void addAssistantRejectsAMechanicBookedOnAnotherVisit() {
        ServiceVisit visit = stubVisit(VisitStatus.SCHEDULED);
        when(appUserService.getByIdOrThrow(ASSISTANT_ID)).thenReturn(assistantUser);
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID)))
                .thenReturn(List.of(assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD)));
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(
                List.of(ASSISTANT_ID), ACTIVE, MONDAY.atTime(9, 0), MONDAY.atTime(11, 0)))
                .thenReturn(List.of(visit(OTHER_VISIT_ID, MONDAY.atTime(10, 30))));

        assertThatThrownBy(() -> serviceVisitService.addAssistant(VISIT_ID, ASSISTANT_ID))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Mechanic is already booked at this time");

        verify(visitAssignmentRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPLETED", "INVOICED"})
    void addAssistantRejectsAVisitThatIsNotScheduledOrInProgress(String status) {
        stubVisit(VisitStatus.valueOf(status));

        assertThatThrownBy(() -> serviceVisitService.addAssistant(VISIT_ID, ASSISTANT_ID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Assistants can only be changed on scheduled or in-progress visits");

        verifyNoInteractions(appUserService, visitAssignmentRepository);
    }

    @Test
    void addAssistantRejectsAFourthAssistant() {
        ServiceVisit visit = stubVisit(VisitStatus.SCHEDULED);
        when(appUserService.getByIdOrThrow(ASSISTANT_ID)).thenReturn(assistantUser);
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID))).thenReturn(List.of(
                assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD),
                assignment(41L, visit, user(6L, "a1", Role.MECHANIC), AssignmentRole.ASSISTANT),
                assignment(42L, visit, user(7L, "a2", Role.MECHANIC), AssignmentRole.ASSISTANT),
                assignment(43L, visit, user(8L, "a3", Role.MECHANIC), AssignmentRole.ASSISTANT)));

        assertThatThrownBy(() -> serviceVisitService.addAssistant(VISIT_ID, ASSISTANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A visit can have at most 3 assistants");

        verify(visitAssignmentRepository, never()).save(any());
    }

    // ---- assistants: remove ----

    @Test
    void removeAssistantDeletesTheAssignment() {
        ServiceVisit visit = stubVisit(VisitStatus.IN_PROGRESS);
        VisitAssignment lead = assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD);
        VisitAssignment assistant = assignment(ASSISTANT_ASSIGNMENT_ID, visit, assistantUser, AssignmentRole.ASSISTANT);
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID)))
                .thenReturn(List.of(lead, assistant));

        ServiceVisitResponseDTO result = serviceVisitService.removeAssistant(VISIT_ID, ASSISTANT_ID);

        verify(visitAssignmentRepository).delete(assistant);
        assertThat(result.mechanics()).singleElement()
                .satisfies(m -> assertThat(m.roleInVisit()).isEqualTo("LEAD"));
    }

    @Test
    void removeAssistantRefusesTheLead() {
        ServiceVisit visit = stubVisit(VisitStatus.SCHEDULED);
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID)))
                .thenReturn(List.of(assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD)));

        assertThatThrownBy(() -> serviceVisitService.removeAssistant(VISIT_ID, MECHANIC_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The lead mechanic cannot be removed");

        verify(visitAssignmentRepository, never()).delete(any());
    }

    @Test
    void removeAssistantGives404ForAMechanicNotOnTheVisit() {
        ServiceVisit visit = stubVisit(VisitStatus.SCHEDULED);
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID)))
                .thenReturn(List.of(assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD)));

        assertThatThrownBy(() -> serviceVisitService.removeAssistant(VISIT_ID, ASSISTANT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Mechanic is not assigned to this visit");
    }

    // ---- complete with assistants ----

    @Test
    void completeStoresTheHoursOfTheLeadAndEveryAssistant() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        visit.setStatus(VisitStatus.IN_PROGRESS);
        VisitAssignment lead = assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD);
        VisitAssignment assistant = assignment(ASSISTANT_ASSIGNMENT_ID, visit, assistantUser, AssignmentRole.ASSISTANT);
        stubCompletingLead(lead, List.of(lead, assistant));
        when(serviceVisitRepository.save(visit)).thenReturn(visit);

        ServiceVisitResponseDTO result = serviceVisitService.completeVisit(VISIT_ID, "Replaced pads",
                new BigDecimal("2.50"), List.of(new AssistantHoursDTO(ASSISTANT_ASSIGNMENT_ID, new BigDecimal("1.25"))),
                "mech");

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.myRole()).isEqualTo("LEAD");
        assertThat(lead.getHoursWorked()).isEqualByComparingTo("2.50");
        assertThat(assistant.getHoursWorked()).isEqualByComparingTo("1.25");
        verify(visitAssignmentRepository).saveAll(List.of(assistant));
    }

    @Test
    void completeRejectsAMissingAssistant() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        visit.setStatus(VisitStatus.IN_PROGRESS);
        VisitAssignment lead = assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD);
        VisitAssignment assistant = assignment(ASSISTANT_ASSIGNMENT_ID, visit, assistantUser, AssignmentRole.ASSISTANT);
        stubCompletingLead(lead, List.of(lead, assistant));

        assertThatThrownBy(() -> serviceVisitService.completeVisit(VISIT_ID, "Replaced pads",
                new BigDecimal("2.50"), null, "mech"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hours are required for every assistant (missing assignment ids: 31)");

        assertThat(visit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
        verify(serviceVisitRepository, never()).save(any());
    }

    @Test
    void completeRejectsTheLeadsOwnAssignmentAsAnAssistant() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        visit.setStatus(VisitStatus.IN_PROGRESS);
        VisitAssignment lead = assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD);
        stubCompletingLead(lead, List.of(lead));

        assertThatThrownBy(() -> serviceVisitService.completeVisit(VISIT_ID, "Replaced pads", new BigDecimal("2.50"),
                List.of(new AssistantHoursDTO(LEAD_ASSIGNMENT_ID, new BigDecimal("1.00"))), "mech"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown assistant assignment: 30");
    }

    @Test
    void completeWithoutAssistantsAcceptsTheOldBody() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        visit.setStatus(VisitStatus.IN_PROGRESS);
        VisitAssignment lead = assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD);
        stubCompletingLead(lead, List.of(lead));
        when(serviceVisitRepository.save(visit)).thenReturn(visit);

        ServiceVisitResponseDTO result = serviceVisitService.completeVisit(
                VISIT_ID, "Oil change", new BigDecimal("1.00"), null, "mech");

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(lead.getHoursWorked()).isEqualByComparingTo("1.00");
    }

    @Test
    void anAssistantCannotCompleteTheVisit() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        visit.setStatus(VisitStatus.IN_PROGRESS);
        when(appUserService.getByUsernameOrThrow("helper")).thenReturn(assistantUser);
        when(visitAssignmentRepository.findByServiceVisitIdAndMechanicId(VISIT_ID, ASSISTANT_ID)).thenReturn(Optional.of(
                assignment(ASSISTANT_ASSIGNMENT_ID, visit, assistantUser, AssignmentRole.ASSISTANT)));

        assertThatThrownBy(() -> serviceVisitService.completeVisit(
                VISIT_ID, "Done", new BigDecimal("1.00"), null, "helper"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Only the lead mechanic can complete a visit that is in progress");

        assertThat(visit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
    }

    // ---- reschedule with an assistant ----

    @Test
    void rescheduleChecksTheAssistantToo() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(9, 0));
        when(serviceVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(List.of(
                assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD),
                assignment(ASSISTANT_ASSIGNMENT_ID, visit, assistantUser, AssignmentRole.ASSISTANT)));
        when(serviceVisitRepository.findByMechanicsAndStatusStartingBetween(
                List.of(MECHANIC_ID, ASSISTANT_ID), ACTIVE, MONDAY.atTime(13, 0), MONDAY.atTime(15, 0)))
                .thenReturn(List.of(visit(OTHER_VISIT_ID, MONDAY.atTime(14, 30))));

        assertThatThrownBy(() -> serviceVisitService.updateVisit(VISIT_ID, new ServiceVisitUpdateDTO(MONDAY.atTime(14, 0))))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Mechanic is already booked at this time");

        verify(serviceVisitRepository, never()).save(any());
    }

    // ---- myRole ----

    @Test
    void leadAndAssistantSeeTheirOwnRoleInTheList() {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        List<VisitAssignment> team = List.of(
                assignment(LEAD_ASSIGNMENT_ID, visit, mechanic, AssignmentRole.LEAD),
                assignment(ASSISTANT_ASSIGNMENT_ID, visit, assistantUser, AssignmentRole.ASSISTANT));
        when(appUserService.getByUsernameOrThrow("mech")).thenReturn(mechanic);
        when(appUserService.getByUsernameOrThrow("helper")).thenReturn(assistantUser);
        when(serviceVisitRepository.findAll(ArgumentMatchers.<Specification<ServiceVisit>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(visit)));
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID))).thenReturn(team);

        ServiceVisitResponseDTO asLead = serviceVisitService.getVisitsForCurrentUser(
                "mech", null, null, null, null, null, null, null, null, null).content().getFirst();
        ServiceVisitResponseDTO asAssistant = serviceVisitService.getVisitsForCurrentUser(
                "helper", null, null, null, null, null, null, null, null, null).content().getFirst();

        assertThat(asLead.myRole()).isEqualTo("LEAD");
        assertThat(asAssistant.myRole()).isEqualTo("ASSISTANT");
        assertThat(asAssistant.mechanics()).extracting(MechanicAssignmentDTO::assignmentId)
                .containsExactly(LEAD_ASSIGNMENT_ID, ASSISTANT_ASSIGNMENT_ID);
        assertThat(asAssistant.mechanics()).extracting(MechanicAssignmentDTO::mechanicId).containsOnlyNulls();
    }

    // ---- helpers ----

    private ServiceVisit stubVisit(VisitStatus status) {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
        visit.setStatus(status);
        when(serviceVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        return visit;
    }

    private void stubCompletingLead(VisitAssignment lead, List<VisitAssignment> team) {
        when(appUserService.getByUsernameOrThrow("mech")).thenReturn(mechanic);
        when(visitAssignmentRepository.findByServiceVisitIdAndMechanicId(VISIT_ID, MECHANIC_ID)).thenReturn(Optional.of(lead));
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(team);
    }

    private static VisitAssignment assignment(Long id, ServiceVisit visit, AppUser user, AssignmentRole role) {
        VisitAssignment assignment = new VisitAssignment();
        assignment.setId(id);
        assignment.setServiceVisit(visit);
        assignment.setMechanic(user);
        assignment.setRoleInVisit(role);
        return assignment;
    }

    private void stubMechanic() {
        when(appUserService.getByIdOrThrow(MECHANIC_ID)).thenReturn(mechanic);
    }

    private static void assertWorkDetails(ServiceVisitResponseDTO dto) {
        assertThat(dto.requestType()).isEqualTo("SERVICE");
        assertThat(dto.requestDescription()).isEqualTo("Brakes squeak when stopping");
        assertThat(dto.vehicleManufacturer()).isEqualTo("Toyota");
        assertThat(dto.vehicleModel()).isEqualTo("Corolla");
    }

    private void stubVisitList(AppUser caller, ServiceVisit visit) {
        when(appUserService.getByUsernameOrThrow(caller.getUsername())).thenReturn(caller);
        when(serviceVisitRepository.findAll(ArgumentMatchers.<Specification<ServiceVisit>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(visit)));
        when(visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(VISIT_ID)))
                .thenReturn(List.of(lead(visit)));
    }

    private VisitAssignment lead(ServiceVisit visit) {
        VisitAssignment lead = new VisitAssignment();
        lead.setServiceVisit(visit);
        lead.setMechanic(mechanic);
        lead.setRoleInVisit(AssignmentRole.LEAD);
        return lead;
    }

    private static AppUser user(Long id, String username, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private void verifyNothingBookedOrSaved() {
        verifyNoInteractions(serviceVisitRepository);
        verifyNoInteractions(visitAssignmentRepository);
    }

    private ServiceVisit stubScheduledVisitWithLead(LocalDateTime start) {
        ServiceVisit visit = visit(VISIT_ID, start);
        when(serviceVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(List.of(lead(visit)));
        return visit;
    }

    private ServiceVisit visit(Long id, LocalDateTime start) {
        ServiceVisit visit = new ServiceVisit();
        visit.setId(id);
        visit.setStatus(VisitStatus.SCHEDULED);
        visit.setScheduledDate(start);
        visit.setVehicle(vehicle);
        visit.setServiceRequest(serviceRequest);
        return visit;
    }

    private static List<LocalTime> unavailableTimes(List<AvailableSlotDTO> slots) {
        return slots.stream()
                .filter(slot -> !slot.available())
                .map(slot -> slot.start().toLocalTime())
                .toList();
    }
}
