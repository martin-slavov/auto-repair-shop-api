package com.example.auto_repair_shop_api.dto.servicerequest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceRequestResponseDTO(Long id, String description, String status, String type,
                                        LocalDateTime createdAt, String licensePlate,
                                        @Schema(description = "Day the customer would like to bring the car",
                                                nullable = true, example = "2026-11-23")
                                        LocalDate preferredDate,
                                        @Schema(description = "MORNING, AFTERNOON or ANY. Null when there is no " +
                                                "preferred date", nullable = true, example = "MORNING")
                                        String preferredPeriod) {
}
