package com.example.auto_repair_shop_api.dto.servicevisit;

import java.math.BigDecimal;

public record MechanicAssignmentDTO(String mechanicName, String roleInVisit, BigDecimal hoursWorked) {
}
