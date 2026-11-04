package com.example.auto_repair_shop_api.model;

import com.example.auto_repair_shop_api.model.enums.AssignmentRole;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "visit_assignment", uniqueConstraints = @UniqueConstraint(columnNames = {"service_visit_id", "mechanic_id"}))
public class VisitAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_visit_id", nullable = false)
    private ServiceVisit serviceVisit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private AppUser mechanic;

    @Column(name = "hours_worked", precision = 5, scale = 2)
    private BigDecimal hoursWorked;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_visit", nullable = false)
    private AssignmentRole roleInVisit;

    public VisitAssignment() {
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

    public AppUser getMechanic() {
        return mechanic;
    }

    public void setMechanic(AppUser mechanic) {
        this.mechanic = mechanic;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public AssignmentRole getRoleInVisit() {
        return roleInVisit;
    }

    public void setRoleInVisit(AssignmentRole roleInVisit) {
        this.roleInVisit = roleInVisit;
    }

    @Override
    public String toString() {
        return "VisitAssignment{" +
                "id=" + id +
                ", hoursWorked=" + hoursWorked +
                ", roleInVisit=" + roleInVisit +
                '}';
    }
}
