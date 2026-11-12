package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.service.InvoiceService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Tag(name = "Invoices", description = "View and pay service invoices")
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "Search invoices (paged). Customers see invoices for their own vehicles; " +
            "admins see all invoices and can filter by 'ownerId'.")
    @GetMapping
    public ResponseEntity<PageResponse<InvoiceResponseDTO>> getInvoicesForCurrentUser(
            @Parameter(description = "Paid state: true or false")
            @RequestParam(required = false) Boolean paid,
            @Parameter(description = "Invoice number contains (case-insensitive)")
            @RequestParam(required = false) String invoiceNumber,
            @Parameter(description = "Vehicle license plate contains (spaces ignored, case-insensitive)")
            @RequestParam(required = false) String licensePlate,
            @Parameter(description = "Issued date from, inclusive (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedFrom,
            @Parameter(description = "Issued date to, inclusive (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedTo,
            @Parameter(description = "Minimum total price (inclusive)")
            @RequestParam(required = false) BigDecimal minTotal,
            @Parameter(description = "Maximum total price (inclusive)")
            @RequestParam(required = false) BigDecimal maxTotal,
            @Parameter(description = "ADMIN only: vehicle owner ID. Ignored for other users.")
            @RequestParam(required = false) Long ownerId,
            @Parameter(description = "Page number, 0-based (default 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 10, max 50)")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "field,asc|desc - fields: invoiceNumber, totalPrice, issuedDate, paid " +
                    "(default issuedDate,desc)")
            @RequestParam(required = false) String sort) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        PageResponse<InvoiceResponseDTO> result = invoiceService.getInvoicesForCurrentUser(
                currentUsername, paid, invoiceNumber, licensePlate, issuedFrom, issuedTo,
                minTotal, maxTotal, ownerId, page, size, sort);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get an invoice by ID (admin: any; customer: own vehicles only)")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDTO> getInvoiceById(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        InvoiceResponseDTO result = invoiceService.getInvoiceById(id, currentUsername);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Delete an unpaid invoice (admin only); the visit goes back to COMPLETED")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {

        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark an invoice as paid (admin only)")
    @PatchMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponseDTO> payInvoice(@PathVariable Long id) {

        InvoiceResponseDTO result = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(result);
    }
}
