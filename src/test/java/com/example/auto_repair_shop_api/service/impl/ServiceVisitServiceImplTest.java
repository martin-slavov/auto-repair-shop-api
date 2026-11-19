package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.config.BookingProperties;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.servicevisit.AvailableSlotDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitUpdateDTO;
import com.example.auto_repair_shop_api.exception.BookingConflictException;
import com.example.auto_repair_shop_api.mapper.ServiceVisitMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
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

        vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setLicensePlate("CA1234AB");

        serviceRequest = new ServiceRequest();
        serviceRequest.setId(3L);
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
        stubVisitList(admin);

        PageResponse<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForCurrentUser(
                "admin", null, null, null, null, null, null, null, null, null);

        assertThat(result.content()).singleElement()
                .satisfies(dto -> assertThat(dto.mechanics()).singleElement()
                        .satisfies(m -> assertThat(m.mechanicId()).isEqualTo(MECHANIC_ID)));
    }

    @Test
    void customerGetsNoMechanicIdInTheVisitList() {
        AppUser customer = user(2L, "owner", Role.CUSTOMER);
        stubVisitList(customer);

        PageResponse<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForCurrentUser(
                "owner", null, null, null, null, null, null, null, null, null);

        assertThat(result.content()).singleElement()
                .satisfies(dto -> assertThat(dto.mechanics()).singleElement().satisfies(m -> {
                    assertThat(m.mechanicName()).isEqualTo("Ivan Petrov");
                    assertThat(m.mechanicId()).isNull();
                }));
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
        assertThat(result.mechanics()).singleElement().satisfies(m -> assertThat(m.mechanicId()).isNull());
    }

    // ---- helpers ----

    private void stubMechanic() {
        when(appUserService.getByIdOrThrow(MECHANIC_ID)).thenReturn(mechanic);
    }

    private void stubVisitList(AppUser caller) {
        ServiceVisit visit = visit(VISIT_ID, MONDAY.atTime(10, 0));
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
        return visit;
    }

    private static List<LocalTime> unavailableTimes(List<AvailableSlotDTO> slots) {
        return slots.stream()
                .filter(slot -> !slot.available())
                .map(slot -> slot.start().toLocalTime())
                .toList();
    }
}
