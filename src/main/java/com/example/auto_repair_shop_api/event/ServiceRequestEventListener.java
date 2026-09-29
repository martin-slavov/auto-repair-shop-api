package com.example.auto_repair_shop_api.event;

import com.example.auto_repair_shop_api.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ServiceRequestEventListener {

    private final EmailService emailService;

    public ServiceRequestEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServiceRequestCreated(ServiceRequestCreatedEvent event) {
        emailService.sendServiceRequestNotification(event.licensePlate(), event.description());
    }
}