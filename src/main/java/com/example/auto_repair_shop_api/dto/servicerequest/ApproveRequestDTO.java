package com.example.auto_repair_shop_api.dto.servicerequest;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ApproveRequestDTO(
        @NotNull(message = "Mechanic ID is required")
        Long mechanicId,

        @NotNull(message = "Scheduled date is required")
        @FutureOrPresent(message = "Scheduled date cannot be in the past")
        LocalDateTime scheduledDate
) {
}
