package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.mapper.InvoiceMapper;
import com.example.auto_repair_shop_api.model.*;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.InvoiceRepository;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final BigDecimal HOURLY_RATE = BigDecimal.valueOf(30.00);

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

        ServiceVisit serviceVisit = serviceVisitRepository.findById(visitId).orElseThrow(() -> new IllegalArgumentException("Visit not found"));

        if (serviceVisit.getStatus() != VisitStatus.COMPLETED) {
            throw new IllegalArgumentException("Visit status must be COMPLETED");
        }

        BigDecimal laborPrice = calculateLaborPrice(visitId);

        BigDecimal partsPrice = calculatePartsPrice(visitId);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setVisit(serviceVisit);
        invoice.setPartsPrice(partsPrice);
        invoice.setLaborPrice(laborPrice);
        invoice.setTotalPrice(partsPrice.add(laborPrice));

        Invoice savedInvoice = invoiceRepository.save(invoice);

        return invoiceMapper.toResponseDto(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceResponseDTO markAsPaid(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        invoice.setPaid(true);
        invoiceRepository.save(invoice);

        return invoiceMapper.toResponseDto(invoice);
    }

    @Override
    public List<InvoiceResponseDTO> getInvoicesForCurrentUser(String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (appUser.getRole() == Role.ADMIN) {
            return invoiceRepository.findAll().stream()
                    .map(invoiceMapper::toResponseDto).toList();
        }

        return invoiceRepository.findByVisitVehicleOwnerId(appUser.getId()).stream()
                .map(invoiceMapper::toResponseDto)
                .toList();
    }

    private String generateInvoiceNumber() {
        long count = invoiceRepository.count() + 1;
        return "INV-" + LocalDate.now().getYear() + "-" + String.format("%05d", count);
    }

    private BigDecimal calculatePartsPrice(Long visitId) {
        List<VisitPart> parts = visitPartRepository.findByServiceVisitId(visitId);

        return parts.stream()
                .map(VisitPart::getPriceAtTimeOfUse)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateLaborPrice(Long visitId) {
        List<VisitAssignment> assignments = visitAssignmentRepository.findByServiceVisitId(visitId);

        double totalHours = assignments.stream()
                .mapToDouble(VisitAssignment::getHoursWorked)
                .sum();

        return HOURLY_RATE.multiply(BigDecimal.valueOf(totalHours));
    }
}
