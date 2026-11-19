package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.invoice.InvoiceDetailDTO;
import com.example.auto_repair_shop_api.dto.invoice.InvoicePartLineDTO;
import com.example.auto_repair_shop_api.dto.invoice.InvoiceResponseDTO;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.mapper.InvoiceMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Invoice;
import com.example.auto_repair_shop_api.model.Part;
import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.VisitPart;
import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import com.example.auto_repair_shop_api.repository.InvoiceRepository;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import com.example.auto_repair_shop_api.repository.VisitPartRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    private static final long VISIT_ID = 12L;
    private static final long INVOICE_ID = 5L;

    @Mock
    private AppUserService appUserService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ServiceVisitRepository serviceVisitRepository;
    @Mock
    private VisitAssignmentRepository visitAssignmentRepository;
    @Mock
    private VisitPartRepository visitPartRepository;

    private InvoiceServiceImpl invoiceService;

    private AppUser owner;
    private AppUser otherCustomer;
    private AppUser admin;
    private ServiceVisit visit;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(appUserService, invoiceRepository, serviceVisitRepository,
                visitAssignmentRepository, visitPartRepository, new InvoiceMapper());

        owner = user(1L, "owner", Role.CUSTOMER);
        otherCustomer = user(2L, "other", Role.CUSTOMER);
        admin = user(3L, "admin", Role.ADMIN);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setLicensePlate("CA1234AB");
        vehicle.setOwner(owner);

        visit = new ServiceVisit();
        visit.setId(VISIT_ID);
        visit.setVehicle(vehicle);
        visit.setStatus(VisitStatus.INVOICED);
    }

    @Test
    void ownerGetsPartLinesAndLabourFields() {
        stubInvoice(invoice("139.15", "112.50"));
        stubUser(owner);
        when(visitPartRepository.findByServiceVisitIdOrderByIdAsc(VISIT_ID)).thenReturn(sampleParts());
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(sampleAssignments());

        InvoiceDetailDTO result = invoiceService.getInvoiceById(INVOICE_ID, "owner");

        assertThat(result.id()).isEqualTo(INVOICE_ID);
        assertThat(result.licensePlate()).isEqualTo("CA1234AB");
        assertThat(result.laborHours()).isEqualByComparingTo("3.75");
        assertThat(result.laborHours().scale()).isEqualTo(2);
        assertThat(result.laborRate()).isEqualByComparingTo("30.00");
        assertThat(result.partLines()).containsExactly(
                new InvoicePartLineDTO("Brake pad set", 2, new BigDecimal("45.00"), new BigDecimal("90.00")),
                new InvoicePartLineDTO("Oil filter", 1, new BigDecimal("12.50"), new BigDecimal("12.50")),
                new InvoicePartLineDTO("Wiper blade", 5, new BigDecimal("7.33"), new BigDecimal("36.65"))
        );
    }

    @Test
    void otherCustomerIsForbiddenAndLinesAreNotLoaded() {
        stubInvoice(invoice("139.15", "112.50"));
        stubUser(otherCustomer);

        assertThatThrownBy(() -> invoiceService.getInvoiceById(INVOICE_ID, "other"))
                .isInstanceOf(OwnershipViolationException.class);

        verify(visitPartRepository, never()).findByServiceVisitIdOrderByIdAsc(anyLong());
        verify(visitAssignmentRepository, never()).findByServiceVisitId(anyLong());
    }

    @Test
    void adminGetsDetailForAnyInvoice() {
        stubInvoice(invoice("139.15", "112.50"));
        stubUser(admin);
        when(visitPartRepository.findByServiceVisitIdOrderByIdAsc(VISIT_ID)).thenReturn(sampleParts());
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(sampleAssignments());

        InvoiceDetailDTO result = invoiceService.getInvoiceById(INVOICE_ID, "admin");

        assertThat(result.invoiceNumber()).isEqualTo("INV-2026-00012");
        assertThat(result.partLines()).hasSize(3);
        assertThat(result.laborHours()).isEqualByComparingTo("3.75");
    }

    @Test
    void totalsOfCreatedInvoiceEqualTheSumOfTheLines() {
        visit.setStatus(VisitStatus.COMPLETED);
        List<VisitPart> parts = sampleParts();
        List<VisitAssignment> assignments = sampleAssignments();
        when(serviceVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(visitPartRepository.findByServiceVisitId(VISIT_ID)).thenReturn(parts);
        when(visitPartRepository.findByServiceVisitIdOrderByIdAsc(VISIT_ID)).thenReturn(parts);
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(assignments);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> {
            Invoice saved = call.getArgument(0);
            saved.setId(INVOICE_ID);
            return saved;
        });

        InvoiceResponseDTO created = invoiceService.createInvoice(VISIT_ID);
        Invoice stored = new Invoice();
        stored.setId(created.id());
        stored.setInvoiceNumber(created.invoiceNumber());
        stored.setVisit(visit);
        stored.setPartsPrice(created.partsPrice());
        stored.setLaborPrice(created.laborPrice());
        stored.setTotalPrice(created.totalPrice());
        stubInvoice(stored);
        stubUser(owner);

        InvoiceDetailDTO detail = invoiceService.getInvoiceById(INVOICE_ID, "owner");

        BigDecimal sumOfLines = detail.partLines().stream()
                .map(InvoicePartLineDTO::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal labour = detail.laborHours().multiply(detail.laborRate());
        assertThat(detail.partsPrice()).isEqualByComparingTo(sumOfLines).isEqualByComparingTo("139.15");
        assertThat(detail.laborPrice()).isEqualByComparingTo(labour).isEqualByComparingTo("112.50");
        assertThat(detail.totalPrice()).isEqualByComparingTo(sumOfLines.add(labour));
        detail.partLines().forEach(line -> assertThat(line.unitPrice()
                .multiply(BigDecimal.valueOf(line.quantity()))).isEqualByComparingTo(line.lineTotal()));
    }

    @Test
    void labourCoversTheLeadAndTheAssistants() {
        visit.setStatus(VisitStatus.COMPLETED);
        VisitAssignment lead = assignment("2.50");
        lead.setRoleInVisit(AssignmentRole.LEAD);
        VisitAssignment assistant = assignment("1.25");
        assistant.setRoleInVisit(AssignmentRole.ASSISTANT);
        VisitAssignment secondAssistant = assignment("0.75");
        secondAssistant.setRoleInVisit(AssignmentRole.ASSISTANT);
        when(serviceVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(visitPartRepository.findByServiceVisitId(VISIT_ID)).thenReturn(List.of());
        when(visitPartRepository.findByServiceVisitIdOrderByIdAsc(VISIT_ID)).thenReturn(List.of());
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(List.of(lead, assistant, secondAssistant));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> {
            Invoice saved = call.getArgument(0);
            saved.setId(INVOICE_ID);
            return saved;
        });

        InvoiceResponseDTO created = invoiceService.createInvoice(VISIT_ID);
        Invoice stored = new Invoice();
        stored.setId(created.id());
        stored.setInvoiceNumber(created.invoiceNumber());
        stored.setVisit(visit);
        stored.setPartsPrice(created.partsPrice());
        stored.setLaborPrice(created.laborPrice());
        stored.setTotalPrice(created.totalPrice());
        stubInvoice(stored);
        stubUser(admin);

        InvoiceDetailDTO detail = invoiceService.getInvoiceById(INVOICE_ID, "admin");

        assertThat(created.laborPrice()).isEqualByComparingTo("135.00");
        assertThat(detail.laborHours()).isEqualByComparingTo("4.50");
        assertThat(detail.laborHours().multiply(detail.laborRate())).isEqualByComparingTo(detail.laborPrice());
    }

    @Test
    void invoiceWithoutPartsReturnsAnEmptyList() {
        stubInvoice(invoice("0.00", "112.50"));
        stubUser(owner);
        when(visitPartRepository.findByServiceVisitIdOrderByIdAsc(VISIT_ID)).thenReturn(List.of());
        when(visitAssignmentRepository.findByServiceVisitId(VISIT_ID)).thenReturn(sampleAssignments());

        InvoiceDetailDTO result = invoiceService.getInvoiceById(INVOICE_ID, "owner");

        assertThat(result.partLines()).isNotNull().isEmpty();
        assertThat(result.partsPrice()).isEqualByComparingTo("0.00");
        assertThat(result.laborHours()).isEqualByComparingTo("3.75");
    }

    private void stubInvoice(Invoice invoice) {
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
    }

    private void stubUser(AppUser user) {
        when(appUserService.getByUsernameOrThrow(user.getUsername())).thenReturn(user);
    }

    private Invoice invoice(String partsPrice, String laborPrice) {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setInvoiceNumber("INV-2026-00012");
        invoice.setVisit(visit);
        invoice.setPartsPrice(new BigDecimal(partsPrice));
        invoice.setLaborPrice(new BigDecimal(laborPrice));
        invoice.setTotalPrice(new BigDecimal(partsPrice).add(new BigDecimal(laborPrice)));
        invoice.setIssuedDate(LocalDateTime.of(2026, 11, 19, 14, 35));
        return invoice;
    }

    private List<VisitPart> sampleParts() {
        return List.of(
                visitPart(1L, "Brake pad set", 2, "90.00"),
                visitPart(2L, "Oil filter", 1, "12.50"),
                visitPart(3L, "Wiper blade", 5, "36.65")
        );
    }

    private List<VisitAssignment> sampleAssignments() {
        return List.of(assignment("2.50"), assignment("1.25"));
    }

    private VisitPart visitPart(Long id, String name, int quantity, String lineTotal) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        VisitPart visitPart = new VisitPart();
        visitPart.setId(id);
        visitPart.setServiceVisit(visit);
        visitPart.setPart(part);
        visitPart.setQuantity(quantity);
        visitPart.setPriceAtTimeOfUse(new BigDecimal(lineTotal));
        return visitPart;
    }

    private VisitAssignment assignment(String hours) {
        VisitAssignment assignment = new VisitAssignment();
        assignment.setServiceVisit(visit);
        assignment.setHoursWorked(new BigDecimal(hours));
        return assignment;
    }

    private static AppUser user(Long id, String username, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
