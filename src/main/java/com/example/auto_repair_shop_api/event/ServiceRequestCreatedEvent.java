package com.example.auto_repair_shop_api.event;

public record ServiceRequestCreatedEvent(String licensePlate, String description) {
}