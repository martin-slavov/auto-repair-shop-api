package com.example.auto_repair_shop_api.dto.servicerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequestCreateDTO(
        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
