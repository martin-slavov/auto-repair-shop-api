package com.example.auto_repair_shop_api.dto.vehicle;

import java.time.LocalDate;

public record VehicleResponseDTO(Long id, String licensePlate, String manufacturer, String model, int year,
                                 Long ownerId, String ownerName, LocalDate firstRegistrationDate) {
}
