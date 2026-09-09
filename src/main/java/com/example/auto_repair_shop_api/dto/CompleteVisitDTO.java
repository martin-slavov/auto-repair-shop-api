package com.example.auto_repair_shop_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteVisitDTO(
        @NotBlank(message = "Technical notes are required")
        String technicalNotes
) {
}