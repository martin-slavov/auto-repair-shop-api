package com.example.auto_repair_shop_api.dto.servicevisit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceVisitResponseDTO(
        Long id, String status, LocalDateTime scheduledDate, LocalDateTime completedDate, String technicalNotes,
        String licensePlate, List<MechanicAssignmentDTO> mechanics,
        @Schema(description = "Type of the service request the visit was created from: SERVICE, INSPECTION, " +
                "CONSULTATION. Null if the visit has no linked request", nullable = true, example = "SERVICE")
        String requestType,
        @Schema(description = "Description of the service request (the work to do). Null if the visit has no " +
                "linked request", nullable = true, example = "Brakes squeak when stopping")
        String requestDescription,
        @Schema(description = "Vehicle manufacturer", example = "Toyota")
        String vehicleManufacturer,
        @Schema(description = "Vehicle model", example = "Corolla")
        String vehicleModel,
        @Schema(description = "The calling mechanic's role in this visit: LEAD or ASSISTANT. Null for ADMIN and " +
                "CUSTOMER", nullable = true, example = "LEAD")
        String myRole) {
}
