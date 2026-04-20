package com.travelplan.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Sent simple email to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Sent HTML email to: {} using template: {}", to, templateName);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
        }
    }

    public void sendWelcomeEmail(String to, String userName) {
        sendHtmlEmail(to, "Welcome to Travel Plan!", "welcome", 
                Map.of("userName", userName));
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        sendHtmlEmail(to, "Password Reset Request", "password-reset",
                Map.of("resetLink", resetLink));
    }

    public void sendBookingConfirmation(String to, String userName, String travelTitle, String confirmationNumber) {
        sendHtmlEmail(to, "Booking Confirmed - " + travelTitle, "booking-confirmation",
                Map.of(
                        "userName", userName,
                        "travelTitle", travelTitle,
                        "confirmationNumber", confirmationNumber
                ));
    }

    public void sendPaymentReceipt(String to, String userName, String amount, String transactionId) {
        sendHtmlEmail(to, "Payment Receipt", "payment-receipt",
                Map.of(
                        "userName", userName,
                        "amount", amount,
                        "transactionId", transactionId
                ));
    }
}
