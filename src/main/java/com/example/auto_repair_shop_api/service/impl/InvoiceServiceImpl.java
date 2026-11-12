package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.InvoiceMapper;
import com.example.auto_repair_shop_api.model.*;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.InvoiceRepository;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.InvoiceService;
import com.example.auto_repair_shop_api.util.PageRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.invoiceNumberContains;
import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.isPaid;
import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.issuedOnOrAfter;
import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.issuedOnOrBefore;
import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.licensePlateContains;
import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.totalAtLeast;
import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.totalAtMost;
import static com.example.auto_repair_shop_api.repository.specification.InvoiceSpecifications.vehicleOwnedBy;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final BigDecimal HOURLY_RATE = new BigDecimal("30.00");

    // Sort whitelist: API field name -> entity property. All four columns are NOT NULL.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "invoiceNumber", "invoiceNumber",
            "totalPrice", "totalPrice",
            "issuedDate", "issuedDate",
            "paid", "paid"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "issuedDate");

    private final AppUserService appUserService;
    private final InvoiceRepository invoiceRepository;
    private final ServiceVisitRepository serviceVisitRepository;
    private final VisitAssignmentRepository visitAssignmentRepository;
    private final VisitPartRepository visitPartRepository;
    private final InvoiceMapper invoiceMapper;

    @Autowired
    public InvoiceServiceImpl(AppUserService appUserService, InvoiceRepository invoiceRepository, ServiceVisitRepository serviceVisitRepository, VisitAssignmentRepository visitAssignmentRepository, VisitPartRepository visitPartRepository, InvoiceMapper invoiceMapper) {
        this.appUserService = appUserService;
        this.invoiceRepository = invoiceRepository;
        this.serviceVisitRepository = serviceVisitRepository;
        this.visitAssignmentRepository = visitAssignmentRepository;
        this.visitPartRepository = visitPartRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Override
    @Transactional
    public InvoiceResponseDTO createInvoice(Long visitId) {

        ServiceVisit serviceVisit = serviceVisitRepository.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        if (serviceVisit.getStatus() != VisitStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Visit status must be COMPLETED");
        }

        BigDecimal laborPrice = calculateLaborPrice(visitId);

        BigDecimal partsPrice = calculatePartsPrice(visitId);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + String.format("%05d", visitId));
        invoice.setVisit(serviceVisit);
        invoice.setPartsPrice(partsPrice);
        invoice.setLaborPrice(laborPrice);
        invoice.setTotalPrice(partsPrice.add(laborPrice));

        Invoice savedInvoice = invoiceRepository.save(invoice);

        serviceVisit.setStatus(VisitStatus.INVOICED);
        serviceVisitRepository.save(serviceVisit);

        return invoiceMapper.toResponseDto(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceResponseDTO markAsPaid(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.isPaid()) {
            throw new InvalidStateTransitionException("Invoice is already paid");
        }

        invoice.setPaid(true);
        invoiceRepository.save(invoice);

        return invoiceMapper.toResponseDto(invoice);
    }

    @Override
    public PageResponse<InvoiceResponseDTO> getInvoicesForCurrentUser(String currentUsername,
                                                                      Boolean paid, String invoiceNumber, String licensePlate,
                                                                      LocalDate issuedFrom, LocalDate issuedTo,
                                                                      BigDecimal minTotal, BigDecimal maxTotal, Long ownerId,
                                                                      Integer page, Integer size, String sort) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);
        boolean isAdmin = appUser.getRole() == Role.ADMIN;

        if (issuedFrom != null && issuedTo != null && issuedFrom.isAfter(issuedTo)) {
            throw new IllegalArgumentException("issuedFrom must not be after issuedTo");
        }
        if (minTotal != null && maxTotal != null && minTotal.compareTo(maxTotal) > 0) {
            throw new IllegalArgumentException("minTotal must not be greater than maxTotal");
        }

        PageRequest pageRequest = PageRequestFactory.create(page, size, sort, SORT_FIELDS, DEFAULT_SORT);

        List<Specification<Invoice>> filters = new ArrayList<>();

        if (!isAdmin) {
            filters.add(vehicleOwnedBy(appUser.getId()));
        } else if (ownerId != null) {
            filters.add(vehicleOwnedBy(ownerId));
        }

        if (paid != null) {
            filters.add(isPaid(paid));
        }
        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            filters.add(invoiceNumberContains(invoiceNumber));
        }
        if (licensePlate != null && !licensePlate.isBlank()) {
            filters.add(licensePlateContains(licensePlate));
        }
        if (issuedFrom != null) {
            filters.add(issuedOnOrAfter(issuedFrom));
        }
        if (issuedTo != null) {
            filters.add(issuedOnOrBefore(issuedTo));
        }
        if (minTotal != null) {
            filters.add(totalAtLeast(minTotal));
        }
        if (maxTotal != null) {
            filters.add(totalAtMost(maxTotal));
        }

        Page<Invoice> result = invoiceRepository.findAll(Specification.allOf(filters), pageRequest);
        return PageResponse.from(result, invoiceMapper::toResponseDto);
    }

    private BigDecimal calculatePartsPrice(Long visitId) {
        List<VisitPart> parts = visitPartRepository.findByServiceVisitId(visitId);

        return parts.stream()
                .map(VisitPart::getPriceAtTimeOfUse)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLaborPrice(Long visitId) {
        List<VisitAssignment> assignments = visitAssignmentRepository.findByServiceVisitId(visitId);

        BigDecimal totalHours = assignments.stream()
                .map(VisitAssignment::getHoursWorked)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return HOURLY_RATE.multiply(totalHours).setScale(2, RoundingMode.HALF_UP);
    }
}
