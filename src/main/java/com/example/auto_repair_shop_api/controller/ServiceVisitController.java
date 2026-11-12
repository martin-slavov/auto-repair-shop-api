package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Service Visits", description = "Mechanic workflow for scheduled visits")
@RestController
@RequestMapping("/api/service-visits")
public class ServiceVisitController {

    private final ServiceVisitService serviceVisitService;
    private final InvoiceService invoiceService;

    public ServiceVisitController(ServiceVisitService serviceVisitService, InvoiceService invoiceService) {
        this.serviceVisitService = serviceVisitService;
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "Search service visits (paged). Admins see all visits, mechanics see visits they are " +
            "assigned to, customers see visits of their own vehicles.")
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

    @Operation(summary = "Update visit status (for instance: from SCHEDULED to IN_PROGRESS)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceVisitResponseDTO> updateVisitStatus(@Valid @RequestBody UpdateVisitStatusDTO dto, @PathVariable Long id) {

        String name = SecurityUtils.getCurrentUsername();
        ServiceVisitResponseDTO result = serviceVisitService.updateVisitStatus(id, dto.newStatus(), name);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Complete a visit with technical notes and hours worked")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ServiceVisitResponseDTO> completeVisit(@PathVariable Long id, @Valid @RequestBody CompleteVisitDTO dto) {

        String name = SecurityUtils.getCurrentUsername();
        ServiceVisitResponseDTO result = serviceVisitService.completeVisit(id, dto.technicalNotes(), dto.hoursWorked(), name);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Generate an invoice for a completed visit (admin only)")
    @PatchMapping("/{id}/invoice")
    public ResponseEntity<InvoiceResponseDTO> createInvoice(@PathVariable Long id) {

        InvoiceResponseDTO result = invoiceService.createInvoice(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Reschedule a scheduled visit (admin only)")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceVisitResponseDTO> updateVisit(@PathVariable Long id, @Valid @RequestBody ServiceVisitUpdateDTO dto) {

        ServiceVisitResponseDTO result = serviceVisitService.updateVisit(id, dto);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Delete a scheduled visit without parts (admin only); the request goes back to PENDING")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable Long id) {

        serviceVisitService.deleteVisit(id);
        return ResponseEntity.noContent().build();
    }
}
