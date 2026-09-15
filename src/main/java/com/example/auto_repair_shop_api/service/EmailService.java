package com.example.auto_repair_shop_api.service;

public interface EmailService {
    void sendServiceRequestNotification(String vehicleLicensePlate, String description);
}