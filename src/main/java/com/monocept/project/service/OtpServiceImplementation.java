package com.monocept.project.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.model.EmailOtp;
import com.monocept.project.model.PhoneOtp;
import com.monocept.project.repository.EmailOtpRepository;
import com.monocept.project.repository.PhoneOtpRepository;
import com.monocept.project.repository.UserRepository;
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

    @Override
    public String sendPhoneOtp(String phone) {

        log.info("Sending phone OTP to {}", phone);

        if(userRepository.findByMobileNumber(phone).isPresent()) {
            log.warn("Mobile number already registered: {}", phone);
            throw new DuplicateResourceException("Mobile number already registered");        }

        try {
            Verification verification = Verification.creator(
                    verifySid,
                    phone,
                    "sms"
            ).create();

            log.info("STATUS {}", verification.getStatus());

            return verification.getStatus();

        } catch(Exception e) {
            log.error("TWILIO ERROR: ", e);
            throw e;
        }



    }

    @Override
    public boolean verifyPhoneOtp(String phone, String otp) {

        log.info("Verifying phone OTP for {}", phone);

        VerificationCheck check = VerificationCheck.creator(verifySid)
                .setTo(phone)
                .setCode(otp)
                .create();

        if(check.getStatus().equals("approved")) {

            phoneOtpRepository.findByPhone(phone)
                    .ifPresent(phoneOtpRepository::delete);

            PhoneOtp phoneOtp = new PhoneOtp();
            phoneOtp.setPhone(phone);
            phoneOtp.setVerified(true);

            phoneOtpRepository.save(phoneOtp);

            log.info("Phone verified successfully: {}", phone);

            return true;
        }

        log.warn("Invalid phone OTP for {}", phone);

        return false;
    }

    @Override
    public void sendEmailOtp(String email) {

        log.info("Sending email OTP to {}", email);

        if(userRepository.findByEmail(email).isPresent()) {
            log.warn("Email already registered: {}", email);
            throw new DuplicateResourceException("Email already registered");        }

        emailOtpRepository.findByEmail(email)
                .ifPresent(emailOtpRepository::delete);

        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));

        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtp(otp);
        emailOtp.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        emailOtp.setVerified(false);

        emailOtpRepository.save(emailOtp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Insurance Verification OTP");
        message.setText("Your OTP is : " + otp);

        mailSender.send(message);

        log.info("Email OTP sent successfully: {}", email);
    }

    @Override
    public boolean verifyEmailOtp(String email, String otp) {

        log.info("Verifying email OTP for {}", email);

        EmailOtp emailOtp = emailOtpRepository.findByEmail(email)
                .orElse(null);

        if(emailOtp == null) {
            log.warn("No OTP found for {}", email);
            return false;
        }

        if(emailOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            emailOtpRepository.delete(emailOtp);
            log.warn("Expired OTP for {}", email);
            return false;
        }

        if(!emailOtp.getOtp().equals(otp)) {
            log.warn("Invalid email OTP for {}", email);
            return false;
        }

        emailOtp.setVerified(true);
        emailOtpRepository.save(emailOtp);

        log.info("Email verified successfully: {}", email);

        return true;
    }
}