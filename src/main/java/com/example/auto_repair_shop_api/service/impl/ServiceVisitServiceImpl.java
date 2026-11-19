package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.config.BookingProperties;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.servicevisit.AssistantHoursDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.AvailableSlotDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitUpdateDTO;
import com.example.auto_repair_shop_api.exception.BookingConflictException;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.ServiceVisitMapper;
import com.example.auto_repair_shop_api.mapper.VisitViewer;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.ServiceVisitService;
import com.example.auto_repair_shop_api.util.PageRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.assignedToMechanic;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.hasStatus;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.hasVehicleId;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.licensePlateContains;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.scheduledOnOrAfter;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.scheduledOnOrBefore;
import static com.example.auto_repair_shop_api.repository.specification.ServiceVisitSpecifications.vehicleOwnedBy;

@Service
public class ServiceVisitServiceImpl implements ServiceVisitService {

    // Sort whitelist: API field name -> entity property.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "scheduledDate", "scheduledDate",
            "completedDate", "completedDate",
            "status", "status"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "scheduledDate");

    // Visits that block a mechanic's time.
    private static final List<VisitStatus> ACTIVE_STATUSES = List.of(VisitStatus.SCHEDULED, VisitStatus.IN_PROGRESS);
    static final String BOOKING_CONFLICT_MESSAGE = "Mechanic is already booked at this time";
    static final int MAX_ASSISTANTS = 3;

    private final ServiceVisitRepository serviceVisitRepository;
    private final VisitAssignmentRepository visitAssignmentRepository;
    private final AppUserService appUserService;
    private final ServiceVisitMapper serviceVisitMapper;
    private final VisitPartRepository visitPartRepository;
    private final BookingProperties booking;
    private final Clock clock;

    @Autowired
    public ServiceVisitServiceImpl(ServiceVisitRepository serviceVisitRepository, VisitAssignmentRepository visitAssignmentRepository, AppUserService appUserService, ServiceVisitMapper serviceVisitMapper,
                                   VisitPartRepository visitPartRepository, BookingProperties booking, Clock clock) {
        this.serviceVisitRepository = serviceVisitRepository;
        this.visitAssignmentRepository = visitAssignmentRepository;
        this.appUserService = appUserService;
        this.serviceVisitMapper = serviceVisitMapper;
        this.visitPartRepository = visitPartRepository;
        this.booking = booking;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ServiceVisitResponseDTO createVisitFromRequest(ServiceRequest serviceRequest, Long mechanicId, LocalDateTime scheduledDate) {

        AppUser mechanic = getEnabledMechanicOrThrow(mechanicId);

        validateSlot(scheduledDate);
        checkNotBooked(List.of(mechanicId), scheduledDate, null);

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

        VisitAssignment savedAssignment = visitAssignmentRepository.save(assignment);

        return serviceVisitMapper.toResponseDto(savedVisit, savedAssignment, VisitViewer.ADMIN);
    }

    @Override
    public PageResponse<ServiceVisitResponseDTO> getVisitsForCurrentUser(String currentUsername,
                                                                         VisitStatus status,
                                                                         LocalDate scheduledFrom, LocalDate scheduledTo,
                                                                         String licensePlate, Long vehicleId, Long mechanicId,
                                                                         Integer page, Integer size, String sort) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (scheduledFrom != null && scheduledTo != null && scheduledFrom.isAfter(scheduledTo)) {
            throw new IllegalArgumentException("scheduledFrom must not be after scheduledTo");
        }

        PageRequest pageRequest = PageRequestFactory.create(page, size, sort, SORT_FIELDS, DEFAULT_SORT);

        List<Specification<ServiceVisit>> filters = new ArrayList<>();

        switch (appUser.getRole()) {
            case ADMIN -> { }
            case MECHANIC -> filters.add(assignedToMechanic(appUser.getId()));
            case CUSTOMER -> filters.add(vehicleOwnedBy(appUser.getId()));
            default -> throw new IllegalStateException("Unsupported role: " + appUser.getRole());
        }

        if (status != null) {
            filters.add(hasStatus(status));
        }
        if (scheduledFrom != null) {
            filters.add(scheduledOnOrAfter(scheduledFrom));
        }
        if (scheduledTo != null) {
            filters.add(scheduledOnOrBefore(scheduledTo));
        }
        if (licensePlate != null && !licensePlate.isBlank()) {
            filters.add(licensePlateContains(licensePlate));
        }
        if (vehicleId != null) {
            filters.add(hasVehicleId(vehicleId));
        }
        // Filtering by mechanic is an admin feature; ignored for other roles.
        if (mechanicId != null && appUser.getRole() == Role.ADMIN) {
            filters.add(assignedToMechanic(mechanicId));
        }

        Page<ServiceVisit> result = serviceVisitRepository.findAll(Specification.allOf(filters), pageRequest);

        Map<Long, List<VisitAssignment>> assignmentsByVisitId = result.hasContent()
                ? visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(
                                result.getContent().stream().map(ServiceVisit::getId).toList()).stream()
                        .collect(Collectors.groupingBy(assignment -> assignment.getServiceVisit().getId()))
                : Map.of();

        VisitViewer viewer = VisitViewer.of(appUser);
        return PageResponse.from(result, visit ->
                serviceVisitMapper.toResponseDto(visit, assignmentsByVisitId.getOrDefault(visit.getId(), List.of()), viewer));
    }

    @Override
    @Transactional
    public ServiceVisitResponseDTO updateVisitStatus(Long visitId, VisitStatus newStatus, String currentUsername) {

        VisitAssignment assignment = getMechanicAssignmentOrThrow(visitId, currentUsername);
        ServiceVisit serviceVisit = assignment.getServiceVisit();

        if (serviceVisit.getStatus() != VisitStatus.SCHEDULED || newStatus != VisitStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Invalid status transition");
        }

        serviceVisit.setStatus(newStatus);
        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, assignment, VisitViewer.of(assignment.getMechanic()));
    }

    @Override
    @Transactional
    public ServiceVisitResponseDTO completeVisit(Long visitId, String technicalNotes, BigDecimal hoursWorked,
                                                 List<AssistantHoursDTO> assistantHours, String currentUsername) {

        VisitAssignment assignment = getMechanicAssignmentOrThrow(visitId, currentUsername);
        ServiceVisit serviceVisit = assignment.getServiceVisit();

        if (serviceVisit.getStatus() != VisitStatus.IN_PROGRESS || !assignment.getRoleInVisit().equals(AssignmentRole.LEAD)) {
            throw new InvalidStateTransitionException("Only the lead mechanic can complete a visit that is in progress");
        }

        List<VisitAssignment> assistants = applyAssistantHours(visitId,
                assistantHours != null ? assistantHours : List.of());

        serviceVisit.setStatus(VisitStatus.COMPLETED);
        serviceVisit.setTechnicalNotes(technicalNotes);
        serviceVisit.setCompletedDate(LocalDateTime.now());

        assignment.setHoursWorked(hoursWorked);
        visitAssignmentRepository.save(assignment);
        visitAssignmentRepository.saveAll(assistants);

        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, assignment, VisitViewer.of(assignment.getMechanic()));
    }

    // ADMIN only: enforced in SecurityConfig.
    @Override
    @Transactional
    public ServiceVisitResponseDTO addAssistant(Long visitId, Long mechanicId) {

        ServiceVisit serviceVisit = getVisitWithEditableTeamOrThrow(visitId);
        AppUser mechanic = getEnabledMechanicOrThrow(mechanicId);
        List<VisitAssignment> assignments = visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(visitId));

        if (assignments.stream().anyMatch(assignment -> assignment.getMechanic().getId().equals(mechanicId))) {
            throw new DuplicateResourceException("Mechanic is already assigned to this visit");
        }

        long assistantCount = assignments.stream()
                .filter(assignment -> assignment.getRoleInVisit() == AssignmentRole.ASSISTANT)
                .count();
        if (assistantCount >= MAX_ASSISTANTS) {
            throw new IllegalArgumentException("A visit can have at most " + MAX_ASSISTANTS + " assistants");
        }

        checkNotBooked(List.of(mechanicId), serviceVisit.getScheduledDate(), visitId);

        VisitAssignment assistant = new VisitAssignment();
        assistant.setServiceVisit(serviceVisit);
        assistant.setMechanic(mechanic);
        assistant.setRoleInVisit(AssignmentRole.ASSISTANT);
        VisitAssignment savedAssistant = visitAssignmentRepository.save(assistant);

        List<VisitAssignment> updated = new ArrayList<>(assignments);
        updated.add(savedAssistant);
        return serviceVisitMapper.toResponseDto(serviceVisit, updated, VisitViewer.ADMIN);
    }

    // ADMIN only: enforced in SecurityConfig.
    @Override
    @Transactional
    public ServiceVisitResponseDTO removeAssistant(Long visitId, Long mechanicId) {

        ServiceVisit serviceVisit = getVisitWithEditableTeamOrThrow(visitId);
        List<VisitAssignment> assignments = visitAssignmentRepository.findByServiceVisitIdInOrderByIdAsc(List.of(visitId));

        VisitAssignment toRemove = assignments.stream()
                .filter(assignment -> assignment.getMechanic().getId().equals(mechanicId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic is not assigned to this visit"));

        if (toRemove.getRoleInVisit() == AssignmentRole.LEAD) {
            throw new IllegalArgumentException("The lead mechanic cannot be removed");
        }

        visitAssignmentRepository.delete(toRemove);

        List<VisitAssignment> remaining = assignments.stream().filter(assignment -> assignment != toRemove).toList();
        return serviceVisitMapper.toResponseDto(serviceVisit, remaining, VisitViewer.ADMIN);
    }

    // ADMIN only: enforced in SecurityConfig.
    @Override
    @Transactional
    public ServiceVisitResponseDTO updateVisit(Long visitId, ServiceVisitUpdateDTO dto) {

        ServiceVisit serviceVisit = getScheduledVisitOrThrow(visitId, "rescheduled");
        List<VisitAssignment> assignments = visitAssignmentRepository.findByServiceVisitId(visitId);

        validateSlot(dto.scheduledDate());
        // Every assigned mechanic (lead or assistant) must be free; the visit itself does not count.
        checkNotBooked(assignments.stream().map(assignment -> assignment.getMechanic().getId()).toList(),
                dto.scheduledDate(), visitId);

        serviceVisit.setScheduledDate(dto.scheduledDate());
        ServiceVisit updatedVisit = serviceVisitRepository.save(serviceVisit);

        return serviceVisitMapper.toResponseDto(updatedVisit, assignments, VisitViewer.ADMIN);
    }

    // ADMIN only: enforced in SecurityConfig.
    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotDTO> getAvailableSlots(Long mechanicId, LocalDate date, Long excludeVisitId) {

        getEnabledMechanicOrThrow(mechanicId);

        List<LocalDateTime> slotStarts = new ArrayList<>();
        for (LocalTime time = booking.getWorkStart(); !time.isAfter(booking.lastStart());
             time = time.plusMinutes(booking.getSlotMinutes())) {
            slotStarts.add(date.atTime(time));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean workingDay = booking.getWorkingDays().contains(date.getDayOfWeek());
        boolean anyFutureSlot = !slotStarts.isEmpty() && !slotStarts.getLast().isBefore(now);

        // One query for the whole day: every active visit that overlaps any of the day's slots.
        List<LocalDateTime> bookedStarts = workingDay && anyFutureSlot
                ? findActiveVisits(List.of(mechanicId), slotStarts.getFirst(), slotStarts.getLast(), excludeVisitId)
                        .stream().map(ServiceVisit::getScheduledDate).toList()
                : List.of();

        return slotStarts.stream()
                .map(start -> new AvailableSlotDTO(start, workingDay
                        && !start.isBefore(now)
                        && bookedStarts.stream().noneMatch(booked -> overlaps(booked, start))))
                .toList();
    }

    // ADMIN only: enforced in SecurityConfig.
    @Override
    @Transactional
    public void deleteVisit(Long visitId) {

        ServiceVisit serviceVisit = getScheduledVisitOrThrow(visitId, "deleted");

        if (visitPartRepository.existsByServiceVisitId(visitId)) {
            throw new ResourceInUseException("Service visit cannot be deleted because it has parts");
        }

        // The request goes back to PENDING so it can be approved again (or edited/deleted by the customer).
        ServiceRequest serviceRequest = serviceVisit.getServiceRequest();
        serviceRequest.setStatus(RequestStatus.PENDING);

        // Assignment rows reference the visit through a foreign key, so they must be removed first.
        visitAssignmentRepository.deleteAll(visitAssignmentRepository.findByServiceVisitId(visitId));
        serviceVisitRepository.delete(serviceVisit);
    }

    private AppUser getEnabledMechanicOrThrow(Long mechanicId) {
        AppUser mechanic = appUserService.getByIdOrThrow(mechanicId);

        if (mechanic.getRole() != Role.MECHANIC) {
            throw new IllegalArgumentException("User with ID " + mechanicId + " is not a mechanic");
        }

        if (!mechanic.isEnabled()) {
            throw new IllegalArgumentException("Mechanic account is disabled");
        }

        return mechanic;
    }

    // Working day, within working hours (the whole visit must fit) and on a slot boundary.
    // "Not in the past" is checked on the request DTOs (@FutureOrPresent).
    private void validateSlot(LocalDateTime start) {
        if (!booking.getWorkingDays().contains(start.getDayOfWeek())) {
            throw new IllegalArgumentException("Scheduled date must be on a working day ("
                    + booking.getWorkingDays().stream().sorted().map(DayOfWeek::name)
                            .collect(Collectors.joining(", ")) + ")");
        }

        LocalTime time = start.toLocalTime();
        if (time.isBefore(booking.getWorkStart()) || time.isAfter(booking.lastStart())) {
            throw new IllegalArgumentException("Scheduled time must be between " + booking.getWorkStart()
                    + " and " + booking.lastStart() + " (working hours " + booking.getWorkStart() + "-"
                    + booking.getWorkEnd() + ", a visit takes " + booking.getVisitDurationMinutes() + " minutes)");
        }

        long minutesFromOpening = Duration.between(booking.getWorkStart(), time).toMinutes();
        if (time.getSecond() != 0 || time.getNano() != 0 || minutesFromOpening % booking.getSlotMinutes() != 0) {
            throw new IllegalArgumentException("Scheduled time must start on a " + booking.getSlotMinutes()
                    + "-minute slot (e.g. " + booking.getWorkStart() + ", "
                    + booking.getWorkStart().plusMinutes(booking.getSlotMinutes()) + ")");
        }
    }

    private void checkNotBooked(List<Long> mechanicIds, LocalDateTime start, Long excludeVisitId) {
        if (!findActiveVisits(mechanicIds, start, start, excludeVisitId).isEmpty()) {
            throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
        }
    }

    /**
     * SCHEDULED / IN_PROGRESS visits of these mechanics that overlap a visit starting anywhere between
     * {@code firstStart} and {@code lastStart}. All visits have the same length D, so two visits overlap exactly
     * when their starts are less than D apart: the query range is (firstStart - D, lastStart + D), both exclusive.
     */
    private List<ServiceVisit> findActiveVisits(List<Long> mechanicIds, LocalDateTime firstStart,
                                                LocalDateTime lastStart, Long excludeVisitId) {
        if (mechanicIds.isEmpty()) {
            return List.of();
        }
        Duration duration = booking.visitDuration();
        return serviceVisitRepository.findByMechanicsAndStatusStartingBetween(mechanicIds, ACTIVE_STATUSES,
                        firstStart.minus(duration), lastStart.plus(duration)).stream()
                .filter(visit -> !Objects.equals(visit.getId(), excludeVisitId))
                .toList();
    }

    private boolean overlaps(LocalDateTime startA, LocalDateTime startB) {
        return Duration.between(startA, startB).abs().compareTo(booking.visitDuration()) < 0;
    }

    private ServiceVisit getVisitWithEditableTeamOrThrow(Long visitId) {
        ServiceVisit serviceVisit = serviceVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Service visit not found with ID: " + visitId));

        if (serviceVisit.getStatus() != VisitStatus.SCHEDULED && serviceVisit.getStatus() != VisitStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Assistants can only be changed on scheduled or in-progress visits");
        }

        return serviceVisit;
    }

    private List<VisitAssignment> applyAssistantHours(Long visitId, List<AssistantHoursDTO> assistantHours) {
        Map<Long, VisitAssignment> assistantsById = visitAssignmentRepository.findByServiceVisitId(visitId).stream()
                .filter(assignment -> assignment.getRoleInVisit() == AssignmentRole.ASSISTANT)
                .collect(Collectors.toMap(VisitAssignment::getId, assignment -> assignment));

        Set<Long> seen = new HashSet<>();
        for (AssistantHoursDTO entry : assistantHours) {
            if (!assistantsById.containsKey(entry.assignmentId())) {
                throw new IllegalArgumentException("Unknown assistant assignment: " + entry.assignmentId());
            }
            if (!seen.add(entry.assignmentId())) {
                throw new IllegalArgumentException("Duplicate assistant assignment: " + entry.assignmentId());
            }
        }

        List<Long> missing = assistantsById.keySet().stream().filter(id -> !seen.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Hours are required for every assistant (missing assignment ids: "
                    + missing.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ")");
        }

        assistantHours.forEach(entry -> assistantsById.get(entry.assignmentId()).setHoursWorked(entry.hoursWorked()));
        return List.copyOf(assistantsById.values());
    }

    private ServiceVisit getScheduledVisitOrThrow(Long visitId, String action) {
        ServiceVisit serviceVisit = serviceVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Service visit not found with ID: " + visitId));

        if (serviceVisit.getStatus() != VisitStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Only scheduled visits can be " + action);
        }

        return serviceVisit;
    }

    private VisitAssignment getMechanicAssignmentOrThrow(Long visitId, String currentUsername) {
        AppUser mechanic = appUserService.getByUsernameOrThrow(currentUsername);
        return visitAssignmentRepository.findByServiceVisitIdAndMechanicId(visitId, mechanic.getId())
                .orElseThrow(() -> new OwnershipViolationException("Mechanic is not assigned to this service visit"));
    }
}
