package com.example.auto_repair_shop_api.dto.servicevisit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AvailableSlotDTO(
        @Schema(description = "Slot start, server local time", example = "2026-11-23T09:30:00")
        LocalDateTime start,

        @Schema(description = "false if the slot is in the past, the mechanic is booked, or the day is not a working day")
        boolean available
) {
}
