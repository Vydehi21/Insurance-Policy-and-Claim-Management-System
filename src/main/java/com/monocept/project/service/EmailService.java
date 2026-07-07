package com.monocept.project.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String sender;

    public void sendPasswordResetEmail(
            String to,
            String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(sender);
        message.setTo(to);
        message.setSubject("Password Reset");

        message.setText("""
                Hello,

                Click the link below to reset your password.

                %s

                This link expires in 15 minutes.

                Regards,
                Insurance Team
                """.formatted(resetLink));

        mailSender.send(message);
    }
}