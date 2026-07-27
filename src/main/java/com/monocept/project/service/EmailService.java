package com.monocept.project.service;

import java.math.BigDecimal;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String sender;

    /**
     * Centralised send + failure handling for all claim lifecycle emails.
     * Runs on the @Async executor thread, so an SMTP failure here is only
     * logged — it must never bubble back up and affect the claim
     * transaction that already committed.
     */
    private void sendClaimEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            log.info("About to send email to {}", to);
            mailSender.send(message);
            log.info("Email successfully sent");
        } catch (Exception e) {
            log.error("Failed to send claim notification email to {} (subject: {})", to, subject, e);
        }
    }

    @Async
    public void sendClaimSubmittedEmail(String to, String customerName, String claimNumber, BigDecimal claimAmount) {
    	log.info("sendClaimSubmittedEmail called for {}", to);
        String subject = "Claim Received - " + claimNumber;
        String body = """
                Hello %s,

                We've received your claim %s for %s and it is now with our internal review team.

                We'll notify you by email as soon as there's an update.

                Regards,
                Insurance Team
                """.formatted(customerName, claimNumber, claimAmount);

        sendClaimEmail(to, subject, body);
    }

    @Async
    public void sendClaimReviewedEmail(String to, String customerName, String claimNumber, String recommendation, String remarks) {
        String subject = "Claim Update - " + claimNumber;
        String friendlyRecommendation = "RECOMMENDED_APPROVAL".equals(recommendation)
                ? "recommended for approval"
                : "recommended for rejection";

        String body = """
                Hello %s,

                Your claim %s has been reviewed by our internal team and %s.

                Reviewer remarks: %s

                It is now pending final decision from our admin team. We'll notify you once that's complete.

                Regards,
                Insurance Team
                """.formatted(customerName, claimNumber, friendlyRecommendation,
                        (remarks == null || remarks.isBlank()) ? "None provided" : remarks);

        sendClaimEmail(to, subject, body);
    }

    @Async
    public void sendClaimDecisionEmail(String to, String customerName, String claimNumber, String decision, String remarks) {
        String subject = "Claim " + (("APPROVED".equals(decision)) ? "Approved" : "Rejected") + " - " + claimNumber;
        String body = """
                Hello %s,

                Your claim %s has been %s by our admin team.

                Remarks: %s

                Regards,
                Insurance Team
                """.formatted(customerName, claimNumber, decision.toLowerCase(),
                        (remarks == null || remarks.isBlank()) ? "None provided" : remarks);

        sendClaimEmail(to, subject, body);
    }

    @Async
    public void sendClaimWithdrawnEmail(String to, String customerName, String claimNumber) {
        String subject = "Claim Withdrawn - " + claimNumber;
        String body = """
                Hello %s,

                Your claim %s has been withdrawn as requested. If this wasn't you, please contact support immediately.

                Regards,
                Insurance Team
                """.formatted(customerName, claimNumber);

        sendClaimEmail(to, subject, body);
    }

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
    
    @Async
    public void sendPolicyPurchaseEmail(
            String to,
            String customerName,
            String policyNumber,
            String planName,
            BigDecimal coverageAmount) {

        String subject = "Policy Purchase Confirmation - " + policyNumber;

        String body = """
                Hello %s,

                Your insurance policy has been created successfully.

                Policy Number: %s
                Plan: %s
                Coverage Amount: ₹%s

                Your policy is currently awaiting premium payment.

                Regards,
                Insurance Team
                """
                .formatted(customerName,
                        policyNumber,
                        planName,
                        coverageAmount);

        sendClaimEmail(to, subject, body);
    }
    
    @Async
    public void sendPremiumPaymentEmail(
            String to,
            String customerName,
            String policyNumber,
            BigDecimal amount) {

        String subject = "Premium Payment Successful - " + policyNumber;

        String body = """
                Hello %s,

                We have successfully received your premium payment.

                Policy Number: %s
                Amount Paid: ₹%s

                Thank you for choosing our insurance services.

                Regards,
                Insurance Team
                """
                .formatted(customerName,
                        policyNumber,
                        amount);

        sendClaimEmail(to, subject, body);
    }
}