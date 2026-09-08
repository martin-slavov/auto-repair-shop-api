package com.example.auto_repair_shop_api.dto;

public record VehicleResponseDTO(Long id, String licensePlate, String manufacturer, String model, int year,
                                 Long ownerId, String ownerName) {
}
