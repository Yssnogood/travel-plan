package com.travelplan.notification.listener;

import com.travelplan.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = "notification.email.welcome")
    public void handleWelcomeEmail(Map<String, String> message) {
        log.info("Received welcome email request for: {}", message.get("email"));
        emailService.sendWelcomeEmail(message.get("email"), message.get("userName"));
    }

    @RabbitListener(queues = "notification.email.password-reset")
    public void handlePasswordResetEmail(Map<String, String> message) {
        log.info("Received password reset email request for: {}", message.get("email"));
        emailService.sendPasswordResetEmail(message.get("email"), message.get("resetLink"));
    }

    @RabbitListener(queues = "notification.email.booking")
    public void handleBookingConfirmation(Map<String, String> message) {
        log.info("Received booking confirmation request for: {}", message.get("email"));
        emailService.sendBookingConfirmation(
                message.get("email"),
                message.get("userName"),
                message.get("travelTitle"),
                message.get("confirmationNumber")
        );
    }

    @RabbitListener(queues = "notification.email.payment")
    public void handlePaymentReceipt(Map<String, String> message) {
        log.info("Received payment receipt request for: {}", message.get("email"));
        emailService.sendPaymentReceipt(
                message.get("email"),
                message.get("userName"),
                message.get("amount"),
                message.get("transactionId")
        );
    }
}
