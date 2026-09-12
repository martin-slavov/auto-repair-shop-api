package com.example.auto_repair_shop_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteVisitDTO(
        @NotBlank(message = "Technical notes are required")
        String technicalNotes,

        @NotNull(message = "Hours worked is required")
        @Positive
        Double hoursWorked
) {
}