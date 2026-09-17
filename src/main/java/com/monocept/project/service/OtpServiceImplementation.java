package com.monocept.project.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.model.EmailOtp;
import com.monocept.project.model.PhoneOtp;
import com.monocept.project.repository.EmailOtpRepository;
import com.monocept.project.repository.PhoneOtpRepository;
import com.monocept.project.repository.UserRepository;
import com.monocept.project.util.LogMaskUtil;
import com.monocept.project.util.PhoneNumberUtil;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImplementation implements OtpService {

    @Value("${twilio.verify.sid}")
    private String verifySid;

    private final JavaMailSender mailSender;
    private final EmailOtpRepository emailOtpRepository;
    private final PhoneOtpRepository phoneOtpRepository;
    private final UserRepository userRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Single source of truth for how long an email OTP stays valid, so the
    // DB expiry check and the text shown to the user can never drift apart.
    private static final int EMAIL_OTP_VALIDITY_MINUTES = 5;

    // Minimum gap between two OTP sends to the same email. Slightly shorter
    // than the 60-second resend timer on the Verify OTP page, so a user who
    // waits for the timer is never rejected by a rounding difference.
    private static final int RESEND_COOLDOWN_SECONDS = 45;

    // Wrong guesses allowed before the OTP is thrown away.
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    @Override
    public String sendPhoneOtp(String phone) {
        String normalizedPhone = PhoneNumberUtil.normalize(phone);
        log.info("Sending phone OTP to {}", LogMaskUtil.phone(normalizedPhone));

        boolean alreadyRegistered = PhoneNumberUtil.variants(normalizedPhone).stream()
                .anyMatch(userRepository::existsByMobileNumber);
        if (alreadyRegistered) {
            log.warn("Phone OTP refused: mobile number already registered");
            throw new DuplicateResourceException("Mobile number already registered");
        }

        try {
            Verification verification = Verification.creator(
                    verifySid,
                    normalizedPhone,
                    "sms"
            ).create();
            log.info("Phone OTP dispatch status: {}", verification.getStatus());
            return verification.getStatus();
        } catch (Exception e) {
            log.error("Twilio error while sending phone OTP", e);
            throw e;
        }
    }

    @Override
    public boolean verifyPhoneOtp(String phone, String otp) {
        String normalizedPhone = PhoneNumberUtil.normalize(phone);
        log.info("Verifying phone OTP for {}", LogMaskUtil.phone(normalizedPhone));

        VerificationCheck check = VerificationCheck.creator(verifySid)
                .setTo(normalizedPhone)
                .setCode(otp)
                .create();

        if ("approved".equals(check.getStatus())) {
            phoneOtpRepository.findByPhone(normalizedPhone)
                    .ifPresent(phoneOtpRepository::delete);

            PhoneOtp phoneOtp = new PhoneOtp();
            phoneOtp.setPhone(normalizedPhone);
            phoneOtp.setVerified(true);
            phoneOtpRepository.save(phoneOtp);

            log.info("Phone verified successfully: {}", LogMaskUtil.phone(normalizedPhone));
            return true;
        }

        log.warn("Invalid phone OTP for {}", LogMaskUtil.phone(normalizedPhone));
        return false;
    }

    @Override
    public void assertResendAllowed(String email) {
        emailOtpRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getCreatedAt() == null) {
                return;
            }
            long secondsSinceLastSend = Duration.between(existing.getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLastSend < RESEND_COOLDOWN_SECONDS) {
                long wait = RESEND_COOLDOWN_SECONDS - secondsSinceLastSend;
                log.warn("OTP resend throttled for {}", LogMaskUtil.email(email));
                throw new BusinessRuleException(
                        "Please wait " + wait + " seconds before requesting another OTP.");
            }
        });
    }

    @Override
    public void sendEmailOtp(String email) {
        log.info("Sending email OTP to {}", LogMaskUtil.email(email));

        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Email OTP refused: email already registered");
            throw new DuplicateResourceException("Email already registered");
        }

        assertResendAllowed(email);

        emailOtpRepository.findByEmail(email)
                .ifPresent(emailOtpRepository::delete);

        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));

        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtp(otp);
        emailOtp.setCreatedAt(LocalDateTime.now());
        emailOtp.setExpiryTime(LocalDateTime.now().plusMinutes(EMAIL_OTP_VALIDITY_MINUTES));
        emailOtp.setVerified(false);
        emailOtp.setAttempts(0);
        emailOtpRepository.save(emailOtp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Insurance Verification OTP");
        message.setText("Your OTP is : " + otp + "\n\nThis OTP is valid for "
                + EMAIL_OTP_VALIDITY_MINUTES + " minutes.");

        mailSender.send(message);

        log.info("Email OTP sent successfully to {}", LogMaskUtil.email(email));
    }

    // REQUIRES_NEW: the attempt counter must be committed even when the caller
    // (e.g. verifyRegister) rolls back its own transaction after a wrong OTP,
    // otherwise the counter would never reach the limit.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessRuleException.class)
    public boolean verifyEmailOtp(String email, String otp) {
        log.info("Verifying email OTP for {}", LogMaskUtil.email(email));

        EmailOtp emailOtp = emailOtpRepository.findByEmail(email).orElse(null);

        if (emailOtp == null) {
            log.warn("No email OTP found for {}", LogMaskUtil.email(email));
            return false;
        }

        if (emailOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            emailOtpRepository.delete(emailOtp);
            log.warn("Expired email OTP for {}", LogMaskUtil.email(email));
            return false;
        }

        if (!emailOtp.getOtp().equals(otp)) {
            int attempts = emailOtp.getAttempts() + 1;
            if (attempts >= MAX_VERIFY_ATTEMPTS) {
                emailOtpRepository.delete(emailOtp);
                log.warn("LOG-014 Email OTP discarded after {} wrong attempts for {}",
                        attempts, LogMaskUtil.email(email));
                throw new BusinessRuleException(
                        "Too many incorrect OTP attempts. Please request a new OTP.");
            }
            emailOtp.setAttempts(attempts);
            emailOtpRepository.save(emailOtp);
            log.warn("Invalid email OTP for {} (attempt {} of {})",
                    LogMaskUtil.email(email), attempts, MAX_VERIFY_ATTEMPTS);
            return false;
        }

        emailOtp.setVerified(true);
        emailOtpRepository.save(emailOtp);

        log.info("Email verified successfully: {}", LogMaskUtil.email(email));
        return true;
    }
}