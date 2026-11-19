package com.example.auto_repair_shop_api.dto.servicevisit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record MechanicAssignmentDTO(
        String mechanicName,
        String roleInVisit,
        BigDecimal hoursWorked,
        @Schema(description = "Mechanic's user id (for GET /api/service-visits/available-slots and removing an " +
                "assistant). Only for ADMIN, null for other roles", nullable = true, example = "4")
        Long mechanicId,
        @Schema(description = "Id of this assignment, used by the lead in PATCH /complete (assistantHours). Only for " +
                "ADMIN and mechanics assigned to the visit, null for others", nullable = true, example = "31")
        Long assignmentId) {
}
