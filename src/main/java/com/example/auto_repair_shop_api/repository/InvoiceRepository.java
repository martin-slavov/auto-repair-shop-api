package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    // The mapper reads visit.vehicle.licensePlate, so load both in the same query
    @Override
    @EntityGraph(attributePaths = {"visit", "visit.vehicle"})
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByVisitVehicleOwnerId(Long ownerId);
}
