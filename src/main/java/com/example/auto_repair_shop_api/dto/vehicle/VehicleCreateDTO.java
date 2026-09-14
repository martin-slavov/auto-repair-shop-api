package com.example.auto_repair_shop_api.dto.vehicle;

import com.example.auto_repair_shop_api.validation.ValidVehicleYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleCreateDTO(
        @NotBlank(message = "License plate is required")
        String licensePlate,

        @NotBlank(message = "Manufacturer is required")
        String manufacturer,

        @NotBlank(message = "Model is required")
        String model,

        @NotNull(message = "Year is required")
        @ValidVehicleYear
        Integer year
) {
}
