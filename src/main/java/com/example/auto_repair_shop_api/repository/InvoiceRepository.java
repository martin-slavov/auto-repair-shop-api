package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByVisitVehicleOwnerId(Long ownerId);
}
