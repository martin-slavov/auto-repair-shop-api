package com.example.auto_repair_shop_api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @OneToOne
    @JoinColumn(name = "visit_id", nullable = false, unique = true)
    private ServiceVisit visit;

    @Column(name = "parts_price", nullable = false)
    private BigDecimal partsPrice;

    @Column(name = "labor_price", nullable = false)
    private BigDecimal laborPrice;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate;

    @Column(name = "paid", nullable = false)
    private boolean paid = false;

    public Invoice() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public ServiceVisit getVisit() {
        return visit;
    }

    public void setVisit(ServiceVisit visit) {
        this.visit = visit;
    }

    public BigDecimal getPartsPrice() {
        return partsPrice;
    }

    public void setPartsPrice(BigDecimal partsPrice) {
        this.partsPrice = partsPrice;
    }

    public BigDecimal getLaborPrice() {
        return laborPrice;
    }

    public void setLaborPrice(BigDecimal laborPrice) {
        this.laborPrice = laborPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDateTime issuedDate) {
        this.issuedDate = issuedDate;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", partsPrice=" + partsPrice +
                ", laborPrice=" + laborPrice +
                ", totalPrice=" + totalPrice +
                ", issuedDate=" + issuedDate +
                ", paid=" + paid +
                '}';
    }
}
