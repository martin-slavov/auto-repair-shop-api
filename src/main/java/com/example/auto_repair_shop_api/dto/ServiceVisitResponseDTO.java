package com.example.auto_repair_shop_api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceVisitResponseDTO(
        Long id, String status, LocalDateTime scheduledDate, LocalDateTime completedDate, String technicalNotes,
        String licensePlate, List<MechanicAssignmentDTO> mechanics) {
}
