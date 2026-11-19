package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.AvailableSlotDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.CompleteVisitDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitUpdateDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.UpdateVisitStatusDTO;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.service.InvoiceService;
import com.example.auto_repair_shop_api.service.ServiceVisitService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Service Visits", description = "Service visit workflow: admins reschedule, delete and invoice visits, " +
        "mechanics work on their assigned visits, customers view the visits of their own vehicles")
@RestController
@RequestMapping("/api/service-visits")
public class ServiceVisitController {

    private final ServiceVisitService serviceVisitService;
    private final InvoiceService invoiceService;

    public ServiceVisitController(ServiceVisitService serviceVisitService, InvoiceService invoiceService) {
        this.serviceVisitService = serviceVisitService;
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "Search service visits (paged)",
            description = "Access: ADMIN, MECHANIC, CUSTOMER. Admins see all visits, mechanics see visits they are " +
                    "assigned to, customers see visits of their own vehicles. `mechanics[].mechanicId` is filled " +
                    "for admins only (null for other roles).")
    @GetMapping
    public ResponseEntity<PageResponse<ServiceVisitResponseDTO>> getVisitsForCurrentUser(
            @Parameter(description = "Visit status: SCHEDULED, IN_PROGRESS, COMPLETED, INVOICED")
            @RequestParam(required = false) VisitStatus status,
            @Parameter(description = "Scheduled date from, inclusive (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledFrom,
            @Parameter(description = "Scheduled date to, inclusive (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledTo,
            @Parameter(description = "Vehicle license plate contains (spaces ignored, case-insensitive)")
            @RequestParam(required = false) String licensePlate,
            @Parameter(description = "Vehicle ID (still limited to the visits the caller may see)")
            @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Assigned mechanic ID (admin only, ignored for other roles)")
            @RequestParam(required = false) Long mechanicId,
            @Parameter(description = "Page number, 0-based (default 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 10, max 50)")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "field,asc|desc - fields: scheduledDate, completedDate, status " +
                    "(default scheduledDate,desc)")
            @RequestParam(required = false) String sort) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        PageResponse<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForCurrentUser(
                currentUsername, status, scheduledFrom, scheduledTo, licensePlate, vehicleId, mechanicId,
                page, size, sort);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Update visit status",
            description = "Access: MECHANIC. Only a mechanic assigned to the visit; the only allowed transition " +
                    "is SCHEDULED to IN_PROGRESS. `mechanics[]` holds only the caller's assignment, with " +
                    "`mechanicId` null.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceVisitResponseDTO> updateVisitStatus(@Valid @RequestBody UpdateVisitStatusDTO dto, @PathVariable Long id) {

        String name = SecurityUtils.getCurrentUsername();
        ServiceVisitResponseDTO result = serviceVisitService.updateVisitStatus(id, dto.newStatus(), name);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Complete a visit",
            description = "Access: MECHANIC. Only the lead mechanic of the visit, while it is IN_PROGRESS. " +
                    "Records the technical notes and hours worked. `mechanics[]` holds only the caller's " +
                    "assignment, with `mechanicId` null.")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ServiceVisitResponseDTO> completeVisit(@PathVariable Long id, @Valid @RequestBody CompleteVisitDTO dto) {

        String name = SecurityUtils.getCurrentUsername();
        ServiceVisitResponseDTO result = serviceVisitService.completeVisit(id, dto.technicalNotes(), dto.hoursWorked(), name);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Generate an invoice for a visit",
            description = "Access: ADMIN. The visit must be COMPLETED.")
    @ApiResponse(responseCode = "201", description = "Invoice created",
            content = @Content(schema = @Schema(implementation = InvoiceResponseDTO.class)))
    @PatchMapping("/{id}/invoice")
    public ResponseEntity<InvoiceResponseDTO> createInvoice(@PathVariable Long id) {

        InvoiceResponseDTO result = invoiceService.createInvoice(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Free slots of a mechanic on a day",
            description = "Access: ADMIN. Returns every slot start of the day (09:00 to 16:00 every 30 minutes, " +
                    "server local time) with `available`. A slot is unavailable if it is in the past, the day is " +
                    "not a working day (Monday to Friday), or the mechanic has a SCHEDULED / IN_PROGRESS visit " +
                    "overlapping it (every visit takes 60 minutes). Pass `excludeVisitId` when rescheduling so the " +
                    "visit's own time counts as free. 404 if the user does not exist, 400 if it is not an " +
                    "enabled mechanic.")
    @GetMapping("/available-slots")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlots(
            @Parameter(description = "Mechanic (user) ID, e.g. `mechanics[].mechanicId` of a visit", required = true)
            @RequestParam Long mechanicId,
            @Parameter(description = "Day (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Visit to ignore (the one being rescheduled)")
            @RequestParam(required = false) Long excludeVisitId) {

        List<AvailableSlotDTO> result = serviceVisitService.getAvailableSlots(mechanicId, date, excludeVisitId);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Reschedule a visit",
            description = "Access: ADMIN. Only SCHEDULED visits. The new start must be now or later, on a working " +
                    "day (Monday to Friday), between 09:00 and 16:00 and on a 30-minute slot (400 otherwise). " +
                    "Every mechanic assigned to the visit must be free for 60 minutes from the new start, " +
                    "ignoring this visit itself (409 \"Mechanic is already booked at this time\"). The response " +
                    "lists every assignment with its `mechanicId`.")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceVisitResponseDTO> updateVisit(@PathVariable Long id, @Valid @RequestBody ServiceVisitUpdateDTO dto) {

        ServiceVisitResponseDTO result = serviceVisitService.updateVisit(id, dto);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Delete a visit",
            description = "Access: ADMIN. Only a SCHEDULED visit without parts; the request goes back to PENDING.")
    @ApiResponse(responseCode = "204", description = "Visit deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable Long id) {

        serviceVisitService.deleteVisit(id);
        return ResponseEntity.noContent().build();
    }
}
