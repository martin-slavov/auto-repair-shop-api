package com.example.auto_repair_shop_api.dto.servicevisit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CompleteVisitDTO(
        @NotBlank(message = "Technical notes are required")
        @Size(max = 5000, message = "Technical notes must not exceed 5000 characters")
        String technicalNotes,

        @NotNull(message = "Hours worked is required")
        @Positive(message = "Hours worked must be a positive number")
        @Digits(integer = 3, fraction = 2, message = "Hours worked must have up to 3 digits and 2 decimal places")
        BigDecimal hoursWorked,

        @Schema(description = "Hours of every ASSISTANT on the visit, exactly one entry each. May be omitted when " +
                "the visit has no assistants", nullable = true)
        List<@Valid @NotNull(message = "Assistant hours entry must not be null") AssistantHoursDTO> assistantHours
) {
}
