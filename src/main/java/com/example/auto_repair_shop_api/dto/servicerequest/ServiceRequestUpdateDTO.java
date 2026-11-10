package com.example.auto_repair_shop_api.dto.servicerequest;

import com.example.auto_repair_shop_api.model.enums.RequestType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ServiceRequestUpdateDTO(
        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @FutureOrPresent(message = "Preferred date cannot be in the past")
        LocalDate preferredDate,

        @NotNull(message = "Type is required")
        RequestType type
) {
}
