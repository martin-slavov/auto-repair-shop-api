package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.service.InvoiceService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponseDTO>> getInvoicesForUserUser() {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<InvoiceResponseDTO> result = invoiceService.getInvoicesForCurrentUser(currentUsername);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponseDTO> payInvoice(@PathVariable Long id) {

        InvoiceResponseDTO result = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(result);
    }
}
