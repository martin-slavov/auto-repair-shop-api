package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.CompleteVisitDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.UpdateVisitStatusDTO;
import com.example.auto_repair_shop_api.service.InvoiceService;
import com.example.auto_repair_shop_api.service.ServiceVisitService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "Get all visits assigned to the current mechanic")
    @GetMapping
    public ResponseEntity<List<ServiceVisitResponseDTO>> getVisitsForMechanic() {

        String name = SecurityUtils.getCurrentUsername();
        List<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForMechanic(name);
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
}
