package com.example.auto_repair_shop_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ServiceRequestCreateDTO {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    public ServiceRequestCreateDTO() {
    }

    public ServiceRequestCreateDTO(String description, Long vehicleId) {
        this.description = description;
        this.vehicleId = vehicleId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }
}
