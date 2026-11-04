package com.example.auto_repair_shop_api.dto.servicevisit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CompleteVisitDTO(
        @NotBlank(message = "Technical notes are required")
        @Size(max = 5000, message = "Technical notes must not exceed 5000 characters")
        String technicalNotes,

        @NotNull(message = "Hours worked is required")
        @Positive(message = "Hours worked must be a positive number")
        Double hoursWorked
) {
}