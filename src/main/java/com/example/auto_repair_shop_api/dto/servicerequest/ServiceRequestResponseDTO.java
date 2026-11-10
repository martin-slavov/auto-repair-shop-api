package com.example.auto_repair_shop_api.dto.servicerequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceRequestResponseDTO(Long id, String description, String status, String type,
                                        LocalDateTime createdAt, String licensePlate, LocalDate preferredDate) {
}
