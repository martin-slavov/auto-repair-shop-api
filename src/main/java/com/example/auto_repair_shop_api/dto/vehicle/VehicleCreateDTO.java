package com.example.auto_repair_shop_api.dto.vehicle;

import com.example.auto_repair_shop_api.validation.ValidVehicleYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleCreateDTO(
        @NotBlank(message = "License plate is required")
        @Size(max = 8, message = "License plate must not exceed 8 characters")
        String licensePlate,

        @NotBlank(message = "Manufacturer is required")
        @Size(max = 50, message = "Manufacturer must not exceed 50 characters")
        String manufacturer,

        @NotBlank(message = "Model is required")
        @Size(max = 80, message = "Model must not exceed 80 characters")
        String model,

        @NotNull(message = "Year is required")
        @ValidVehicleYear
        Integer year
) {
}
