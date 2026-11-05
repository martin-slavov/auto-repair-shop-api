package com.example.auto_repair_shop_api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "visit_part")
public class VisitPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_visit_id", nullable = false)
    private ServiceVisit serviceVisit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price_at_time_of_use", nullable = false)
    private BigDecimal priceAtTimeOfUse;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public VisitPart() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceVisit getServiceVisit() {
        return serviceVisit;
    }

    public void setServiceVisit(ServiceVisit serviceVisit) {
        this.serviceVisit = serviceVisit;
    }

    public Part getPart() {
        return part;
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtTimeOfUse() {
        return priceAtTimeOfUse;
    }

    public void setPriceAtTimeOfUse(BigDecimal priceAtTimeOfUse) {
        this.priceAtTimeOfUse = priceAtTimeOfUse;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public String toString() {
        return "VisitPart{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", priceAtTimeOfUse=" + priceAtTimeOfUse +
                ", addedAt=" + addedAt +
                '}';
    }
}
