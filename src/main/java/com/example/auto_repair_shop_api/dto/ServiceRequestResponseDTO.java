package com.example.auto_repair_shop_api.dto;

import java.time.LocalDateTime;

public record ServiceRequestResponseDTO(Long id, String description, String status, LocalDateTime createdAt,
                                        String licensePlate) {
}
