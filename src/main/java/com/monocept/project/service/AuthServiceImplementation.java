package com.monocept.project.service;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.ForgotPasswordRequestDTO;
import com.monocept.project.dto.LoginRequestDTO;
import com.monocept.project.dto.LoginResponseDTO;
import com.monocept.project.dto.RegistrationRequestDTO;
import com.monocept.project.dto.ResetPasswordRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.exception.AuthenticationException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.model.EmailOtp;
import com.monocept.project.model.PhoneOtp;
import com.monocept.project.model.User;
import com.monocept.project.repository.EmailOtpRepository;
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
    private final EmailOtpRepository emailOtpRepository;
    private final PhoneOtpRepository phoneOtpRepository;

    @Override
    @Transactional
    public UserResponseDTO registerCustomer(RegistrationRequestDTO registrationRequestDTO) {

        log.info("Registering customer: {}", registrationRequestDTO.getEmail());

        if (userRepository.existsByEmail(registrationRequestDTO.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already exists: " + registrationRequestDTO.getEmail());
        }

        EmailOtp emailOtp = emailOtpRepository
                .findByEmail(registrationRequestDTO.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Please verify email first"));

        if(!emailOtp.isVerified()) {
            throw new RuntimeException("Please verify email first");
        }

        PhoneOtp phoneOtp = phoneOtpRepository
                .findByPhone(registrationRequestDTO.getMobileNumber())
                .orElseThrow(() ->
                        new RuntimeException("Please verify phone number first"));

        if(!phoneOtp.isVerified()) {
            throw new RuntimeException("Please verify phone number first");
        }

        User user = modelMapper.map(registrationRequestDTO, User.class);

        user.setRole(Role.CUSTOMER);
        user.setActiveStatus(true);

        user.setPassword(
                passwordEncoder.encode(registrationRequestDTO.getPassword())
        );

        User savedUser = userRepository.save(user);

        log.info("User registered with id: {}", savedUser.getId());

        return modelMapper.map(savedUser, UserResponseDTO.class);
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
    
    private String generateOtp() {

        return String.valueOf(
                100000 +
                new java.util.Random()
                        .nextInt(900000));
    }
    
    @Override
    @Transactional
    public String forgotPassword(
            ForgotPasswordRequestDTO request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new AuthenticationException("User not found"));

        String otp = generateOtp();

        user.setResetPasswordOtp(otp);
        user.setResetPasswordOtpExpiry(
                LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        return otp;
    }
    
    @Override
    @Transactional
    public void resetPassword(
            ResetPasswordRequestDTO request) {

        User user =
                userRepository
                        .findByEmail(
                                request.getEmail())
                        .orElseThrow(
                                () -> new AuthenticationException(
                                        "User not found"));

        if (!request.getOtp()
                .equals(
                        user.getResetPasswordOtp())) {

            throw new AuthenticationException(
                    "Invalid OTP");
        }

        if (user.getResetPasswordOtpExpiry()
                .isBefore(
                        LocalDateTime.now())) {

            throw new AuthenticationException(
                    "OTP expired");
        }

        user.setPassword(
                request.getNewPassword());

        user.setResetPasswordOtp(null);

        user.setResetPasswordOtpExpiry(null);

        userRepository.save(user);
    }
}
