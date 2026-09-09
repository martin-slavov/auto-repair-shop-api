package com.example.auto_repair_shop_api.dto;

import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVisitStatusDTO(
        @NotNull(message = "Status is required")
        VisitStatus newStatus
) {
}
