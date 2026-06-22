package com.monocept.project.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.monocept.project.dto.ForgotPasswordRequestDTO;
import com.monocept.project.dto.LoginRequestDTO;
import com.monocept.project.dto.LoginResponseDTO;
import com.monocept.project.dto.RegistrationRequestDTO;
import com.monocept.project.dto.ResendRegistrationOtpDTO;
import com.monocept.project.dto.ResetPasswordRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.VerifyRegistrationOtpDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.exception.AuthenticationException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.model.EmailOtp;
import com.monocept.project.model.PendingUser;
import com.monocept.project.model.PhoneOtp;
import com.monocept.project.model.User;

import com.monocept.project.repository.CustomerRepository;

import com.monocept.project.repository.EmailOtpRepository;
import com.monocept.project.repository.PendingUserRepository;
import com.monocept.project.repository.PhoneOtpRepository;

import com.monocept.project.repository.UserRepository;
import com.monocept.project.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImplementation implements AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final CustomerRepository customerRepository;
    private final ResendEmailService resendEmailService;
    
    @Value("${app.frontend.reset-url}")
    private String resetUrl;

    private final EmailOtpRepository emailOtpRepository;
    private final PhoneOtpRepository phoneOtpRepository;
    private final OtpService otpService;
    private final PendingUserRepository pendingUserRepository;


    @Override
    @Transactional
    public String registerCustomer(RegistrationRequestDTO registrationRequestDTO) {

        log.info("Registering customer: {}", registrationRequestDTO.getEmail());

        if (userRepository.existsByEmail(registrationRequestDTO.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already exists: " + registrationRequestDTO.getEmail());
        }
        
        if (userRepository.existsByMobileNumber(registrationRequestDTO.getMobileNumber())) {
            throw new DuplicateResourceException(
                    "Phone number already exists: " + registrationRequestDTO.getMobileNumber());
        }
//
//        EmailOtp emailOtp = emailOtpRepository
//                .findByEmail(registrationRequestDTO.getEmail())
//                .orElseThrow(() ->
//                        new RuntimeException("Please verify email first"));
//
//        if(!emailOtp.isVerified()) {
//            throw new RuntimeException("Please verify email first");
//        }
//
//        PhoneOtp phoneOtp = phoneOtpRepository
//                .findByPhone(registrationRequestDTO.getMobileNumber())
//                .orElseThrow(() ->
//                        new RuntimeException("Please verify phone number first"));
//
//        if(!phoneOtp.isVerified()) {
//            throw new RuntimeException("Please verify phone number first");
//        }
//
//        User user = modelMapper.map(registrationRequestDTO, User.class);
//
//        user.setRole(Role.CUSTOMER);
//        user.setActiveStatus(true);
//
//        user.setPassword(
//                passwordEncoder.encode(registrationRequestDTO.getPassword())
//        );
//
//        User savedUser = userRepository.save(user);
//
//        log.info("User registered with id: {}", savedUser.getId());
//
//        return modelMapper.map(savedUser, UserResponseDTO.class);
        
        if(pendingUserRepository.existsByEmail(registrationRequestDTO.getEmail())) {
            throw new DuplicateResourceException(
                    "Registration already pending. Please verify OTP or resend OTP"
            );
        }

        if(pendingUserRepository.existsByMobileNumber(registrationRequestDTO.getMobileNumber())) {
            throw new DuplicateResourceException(
                    "Registration already pending. Please verify OTP or resend OTP"
            );
        }
        
        PendingUser pendingUser =
                modelMapper.map(registrationRequestDTO, PendingUser.class);

        pendingUser.setPassword(
                passwordEncoder.encode(registrationRequestDTO.getPassword())
        );

        pendingUserRepository.save(pendingUser);
        
        otpService.sendEmailOtp(registrationRequestDTO.getEmail());
        otpService.sendPhoneOtp(registrationRequestDTO.getMobileNumber());
        
        return "OTP sent successfully";
    }
    
    @Override
    @Transactional
    public UserResponseDTO verifyRegister(
            VerifyRegistrationOtpDTO dto) {

        EmailOtp emailOtp =
                emailOtpRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Email OTP not found"));

        if(!emailOtp.getOtp().equals(dto.getEmailOtp())) {
            throw new RuntimeException("Invalid Email OTP");
        }
        
        emailOtp.setVerified(true);
        emailOtpRepository.save(emailOtp);

        boolean phoneVerified =
                otpService.verifyPhoneOtp(
                        dto.getMobileNumber(),
                        dto.getPhoneOtp()
                );

        if(!phoneVerified) {

            throw new RuntimeException("Invalid Phone OTP");
        }

        PendingUser pendingUser =
                pendingUserRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Registration expired"));

        User user =
                modelMapper.map(
                        pendingUser,
                        User.class
                );

        user.setRole(Role.CUSTOMER);
        user.setActiveStatus(true);

        User savedUser = userRepository.save(user);
        
     // CLEAN UP OTP RECORDS
        emailOtpRepository.delete(emailOtp);
        phoneOtpRepository
        .findByPhone(dto.getMobileNumber())
        .ifPresent(phoneOtpRepository::delete);

        pendingUserRepository.delete(pendingUser);

        return modelMapper.map(
                savedUser,
                UserResponseDTO.class
        );
    }
    
    @Override
    public String resendRegistrationOtp(
            ResendRegistrationOtpDTO dto){

        PendingUser pendingUser =
                pendingUserRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                        "Please register first"));

        otpService.sendEmailOtp(
                pendingUser.getEmail()
        );
        
        otpService.sendPhoneOtp(
                pendingUser.getMobileNumber()
        );

        return "OTP resent successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        log.info("Login attempt: {}", loginRequestDTO.getEmail());

        User user = userRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed. Email not found: {}", loginRequestDTO.getEmail());
                    return new AuthenticationException("Invalid credentials");
                });

        // FIXED: Safely verify hashed passwords instead of standard text equals comparisons
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            log.warn("Wrong password attempt for email: {}", user.getEmail());
            throw new AuthenticationException("Invalid credentials");
        }

        if (!user.getActiveStatus()) {
            log.warn("Login failed. Inactive account access attempt. User id: {}", user.getId());
            throw new AuthenticationException("Inactive user account");
        }

        String jwtToken = jwtService.generateToken(user);

        // Populate Response DTO clean structure
        LoginResponseDTO response = new LoginResponseDTO();
        response.setJwtToken(jwtToken);
        response.setTokenType("Bearer");
        response.setUserEmail(user.getEmail());
        response.setUserRole(user.getRole());
        response.setTokenExpiryInformation(System.currentTimeMillis() + 86400000L); // 24-hour expiration window

        log.info("Login successful for user id: {}", user.getId());
        return response;
    }
    
    @Override
    @Transactional
    public void forgotPassword(
            ForgotPasswordRequestDTO request) {

        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new AuthenticationException(
                                        "User not found"));

        String token =
                UUID.randomUUID().toString();

        user.setResetToken(token);

        user.setResetTokenExpiry(
                LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        String fullResetUrl =
                resetUrl + "?token=" + token;

        log.info("Reset URL: {}", fullResetUrl);

        try {

            resendEmailService.sendPasswordResetEmail(
                    user.getEmail(),
                    fullResetUrl);

        } catch (IOException | InterruptedException e) {

            log.error(
                    "Failed to send password reset email",
                    e);

            throw new RuntimeException(
                    "Unable to send reset email");
        }
    }
    
    @Override
    @Transactional
    public void resetPassword(
            ResetPasswordRequestDTO request) {

        User user =
                userRepository
                .findByResetToken(
                        request.getToken())
                        .orElseThrow(
                                () -> new AuthenticationException(
                                        "User not found"));
        
        if(!passwordEncoder.matches(
                request.getToken(),
                user.getResetToken())) {

            throw new AuthenticationException(
                    "Invalid token");
        }

        if (user.getResetTokenExpiry()
                .isBefore(
                        LocalDateTime.now())) {

            throw new AuthenticationException(
                    "Reset token expired");
        }

        // Encrypt the raw password before saving it to MySQL
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        user.setResetToken(null);

        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }
    
   
    
    
}
