package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.service.InvoiceService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Invoices", description = "View and pay service invoices")
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "Get invoices (all for admin, own for customer)")
    @GetMapping
    public ResponseEntity<List<InvoiceResponseDTO>> getInvoicesForUserUser() {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<InvoiceResponseDTO> result = invoiceService.getInvoicesForCurrentUser(currentUsername);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Mark an invoice as paid (admin only)")
    @PatchMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponseDTO> payInvoice(@PathVariable Long id) {

        InvoiceResponseDTO result = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(result);
    }
}
