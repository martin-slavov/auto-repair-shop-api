package com.example.auto_repair_shop_api.dto.servicerequest;

import com.example.auto_repair_shop_api.model.enums.PreferredPeriod;
import com.example.auto_repair_shop_api.model.enums.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ServiceRequestCreateDTO(
        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Schema(description = "Day the customer would like to bring the car: today up to 90 days ahead, on a " +
                "working day", nullable = true, example = "2026-11-23")
        LocalDate preferredDate,

        // Optional: when null, the service uses RequestType.SERVICE
        RequestType type,

        @Schema(description = "MORNING, AFTERNOON or ANY. Requires preferredDate; defaults to ANY when a date " +
                "is given", nullable = true, example = "MORNING")
        PreferredPeriod preferredPeriod
) {
}
