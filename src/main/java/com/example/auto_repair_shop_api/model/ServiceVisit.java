package com.example.auto_repair_shop_api.model;

import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_visit")
public class ServiceVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VisitStatus status;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "technical_notes", columnDefinition = "TEXT")
    private String technicalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    public ServiceVisit() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitStatus status) {
        this.status = status;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public String getTechnicalNotes() {
        return technicalNotes;
    }

    public void setTechnicalNotes(String technicalNotes) {
        this.technicalNotes = technicalNotes;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    @Override
    public String toString() {
        return "ServiceVisit{" +
                "id=" + id +
                ", status=" + status +
                ", scheduledDate=" + scheduledDate +
                ", completedDate=" + completedDate +
                ", technicalNotes='" + technicalNotes + '\'' +
                '}';
    }
}
