package com.example.auto_repair_shop_api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "part")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "unit_sell_price", nullable = false)
    private BigDecimal unitSellPrice;

    public Part() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getUnitSellPrice() {
        return unitSellPrice;
    }

    public void setUnitSellPrice(BigDecimal unitSellPrice) {
        this.unitSellPrice = unitSellPrice;
    }

    @Override
    public String toString() {
        return "Part{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", unitCost=" + unitCost +
                ", unitSellPrice=" + unitSellPrice +
                '}';
    }
}
