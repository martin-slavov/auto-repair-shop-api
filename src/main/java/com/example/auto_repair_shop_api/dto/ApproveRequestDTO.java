package com.example.auto_repair_shop_api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ApproveRequestDTO(
        @NotNull(message = "Mechanic ID is required")
        Long mechanicId,

        @NotNull(message = "Scheduled date is required")
        LocalDateTime scheduledDate
) {
}
