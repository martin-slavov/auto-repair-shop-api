package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InvoiceService {

    InvoiceResponseDTO createInvoice(Long visitId);

    InvoiceResponseDTO markAsPaid(Long invoiceId);

    InvoiceResponseDTO getInvoiceById(Long invoiceId, String currentUsername);

    void deleteInvoice(Long invoiceId);

    PageResponse<InvoiceResponseDTO> getInvoicesForCurrentUser(String currentUsername,
                                                               Boolean paid, String invoiceNumber, String licensePlate,
                                                               LocalDate issuedFrom, LocalDate issuedTo,
                                                               BigDecimal minTotal, BigDecimal maxTotal, Long ownerId,
                                                               Integer page, Integer size, String sort);
}
