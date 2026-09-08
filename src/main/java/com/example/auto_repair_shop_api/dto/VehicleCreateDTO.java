package com.example.auto_repair_shop_api.dto;

import com.example.auto_repair_shop_api.validation.ValidVehicleYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VehicleCreateDTO {

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    @NotBlank(message = "Model is required")
    private String model;

    @NotNull(message = "Year is required")
    @ValidVehicleYear
    private Integer year;

    public VehicleCreateDTO() {
    }

    public VehicleCreateDTO(String licensePlate, String manufacturer, String model, Integer year) {
        this.licensePlate = licensePlate;
        this.manufacturer = manufacturer;
        this.model = model;
        this.year = year;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
