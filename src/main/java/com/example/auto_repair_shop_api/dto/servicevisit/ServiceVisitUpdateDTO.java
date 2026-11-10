package com.example.auto_repair_shop_api.dto.servicevisit;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ServiceVisitUpdateDTO(
        @NotNull(message = "Scheduled date is required")
        @FutureOrPresent(message = "Scheduled date cannot be in the past")
        LocalDateTime scheduledDate
) {
}
