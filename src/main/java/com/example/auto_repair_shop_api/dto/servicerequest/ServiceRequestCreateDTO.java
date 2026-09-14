package com.example.auto_repair_shop_api.dto.servicerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceRequestCreateDTO(
        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,

        @NotBlank(message = "Description is required")
        String description
) {
}
