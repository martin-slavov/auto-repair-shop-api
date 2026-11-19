package com.example.auto_repair_shop_api.dto.servicevisit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AssistantHoursDTO(
        @Schema(description = "mechanics[].assignmentId of an ASSISTANT on this visit", example = "31")
        @NotNull(message = "Assignment ID is required")
        Long assignmentId,

        @Schema(example = "1.25")
        @NotNull(message = "Hours worked is required")
        @Positive(message = "Hours worked must be a positive number")
        @Digits(integer = 3, fraction = 2, message = "Hours worked must have up to 3 digits and 2 decimal places")
        BigDecimal hoursWorked
) {
}
