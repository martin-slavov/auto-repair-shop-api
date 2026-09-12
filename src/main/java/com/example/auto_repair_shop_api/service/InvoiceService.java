package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.InvoiceResponseDTO;

import java.util.List;

public interface InvoiceService {

    InvoiceResponseDTO createInvoice(Long visitId);

    InvoiceResponseDTO markAsPaid(Long invoiceId);

    List<InvoiceResponseDTO> getInvoicesForCurrentUser(String currentUsername);
}
