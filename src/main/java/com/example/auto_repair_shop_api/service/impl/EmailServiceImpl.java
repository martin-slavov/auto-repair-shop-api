package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendServiceRequestNotification(String vehicleLicensePlate, String description) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("admin@autorepairshop.com");
        message.setSubject("New Service Request - " + vehicleLicensePlate);
        message.setText("A new service request has been submitted.\n\n"
                + "Vehicle: " + vehicleLicensePlate + "\n"
                + "Description: " + description);

        mailSender.send(message);
    }
}