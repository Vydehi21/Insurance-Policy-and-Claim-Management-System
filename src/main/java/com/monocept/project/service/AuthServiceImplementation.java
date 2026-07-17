package com.monocept.project.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.monocept.project.exception.InvalidRequestException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.PendingUser;
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
	private final EmailService emailService;

	@Value("${app.frontend.reset-url}")
	private String resetUrl;
	
	@Value("${jwt.expiration}")
	  private Long jwtExpiration;

	private final EmailOtpRepository emailOtpRepository;
	private final PhoneOtpRepository phoneOtpRepository;
	private final OtpService otpService;
	private final PendingUserRepository pendingUserRepository;
	
	@Override
	@Transactional
	public String registerCustomer(RegistrationRequestDTO registrationRequestDTO) {

		log.info("Registering customer: {}", registrationRequestDTO.getEmail());

		if (userRepository.existsByEmail(registrationRequestDTO.getEmail())) {
			throw new DuplicateResourceException("Email already exists: " + registrationRequestDTO.getEmail());
		}

		if (userRepository.existsByMobileNumber(registrationRequestDTO.getMobileNumber())) {
			throw new DuplicateResourceException(
					"Phone number already exists: " + registrationRequestDTO.getMobileNumber());
		}

		if (pendingUserRepository.existsByEmail(registrationRequestDTO.getEmail())) {
			throw new DuplicateResourceException("Registration already pending. Please verify OTP or resend OTP");
		}

		if (pendingUserRepository.existsByMobileNumber(registrationRequestDTO.getMobileNumber())) {
			throw new DuplicateResourceException("Registration already pending. Please verify OTP or resend OTP");
		}

		otpService.sendEmailOtp(registrationRequestDTO.getEmail());
		otpService.sendPhoneOtp(registrationRequestDTO.getMobileNumber());

		PendingUser pendingUser = modelMapper.map(registrationRequestDTO, PendingUser.class);

		pendingUser.setPassword(
		    passwordEncoder.encode(registrationRequestDTO.getPassword())
		);

		pendingUserRepository.save(pendingUser);

		return "OTP sent successfully";
	}

	@Override
	@Transactional
	public UserResponseDTO verifyRegister(VerifyRegistrationOtpDTO dto) {
	    log.info("Verifying registration OTP challenge for email: {}", dto.getEmail());

	    boolean emailVerified = otpService.verifyEmailOtp(dto.getEmail(), dto.getEmailOtp());
	    if (!emailVerified) {
	        log.warn("Registration verification failed. Invalid or expired email OTP: {}", dto.getEmail());
	        throw new InvalidRequestException("Invalid or expired email OTP");
	    }

	    boolean phoneVerified = otpService.verifyPhoneOtp(dto.getMobileNumber(), dto.getPhoneOtp());
	    if (!phoneVerified) {
	        log.warn("Registration verification failed. Invalid phone OTP: {}", dto.getMobileNumber());
	        throw new InvalidRequestException("Invalid Phone OTP");
	    }

	    PendingUser pendingUser = pendingUserRepository.findByEmail(dto.getEmail())
	            .orElseThrow(() -> new ResourceNotFoundException("Registration expired. Please register again"));

	    User user = modelMapper.map(pendingUser, User.class);

	    // CRITICAL: PendingUser's own primary key must never leak into the new User's primary key.
	    // ModelMapper copies same-named/same-typed fields by default, and both entities happen to
	    // have a "Long id" field — without this reset, save() treats the new user as an UPDATE
	    // to whatever existing row shares that id (e.g. the very first user row: your admin).
	    user.setId(null);

	    user.setRole(Role.CUSTOMER);
	    user.setActiveStatus(true);

	    User savedUser = userRepository.save(user);

	    emailOtpRepository.findByEmail(dto.getEmail()).ifPresent(emailOtpRepository::delete);
	    phoneOtpRepository.findByPhone(dto.getMobileNumber()).ifPresent(phoneOtpRepository::delete);
	    pendingUserRepository.delete(pendingUser);

	    log.info("Customer registration completed successfully. User id: {}", savedUser.getId());

	    return modelMapper.map(savedUser, UserResponseDTO.class);
	}
	
	@Override
	public String resendRegistrationOtp(ResendRegistrationOtpDTO dto) {

		PendingUser pendingUser = pendingUserRepository.findByEmail(dto.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException(
						                      "No pending registration found. Please register first"));

		otpService.sendEmailOtp(pendingUser.getEmail());

		otpService.sendPhoneOtp(pendingUser.getMobileNumber());
		
		log.info("Registration OTP resent for pending email: {}", dto.getEmail());

		return "OTP resent successfully";
	}

	@Override
	@Transactional(readOnly = true)
	public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
		log.info("Login attempt: {}", loginRequestDTO.getEmail());

		User user = userRepository.findByEmail(loginRequestDTO.getEmail()).orElseThrow(() -> {
			log.warn("Login failed. Email not found: {}", loginRequestDTO.getEmail());
			return new AuthenticationException("Invalid credentials");
		});

		// FIXED: Safely verify hashed passwords instead of standard text equals
		// comparisons
				
		if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword()))  {
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
		response.setTokenExpiryInformation(System.currentTimeMillis() + jwtExpiration); // 24-hour expiration window

		log.info("Login successful for user id: {}", user.getId());
		return response;
	}

	@Override
	@Transactional
	public void forgotPassword(ForgotPasswordRequestDTO request) {

	    // Look up the account, but don't throw if it's missing — silently do nothing
	    // for unregistered emails so the controller's generic response stays truthful
	    // for both cases (prevents user enumeration).
	    userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

	        // 1. Generate a secure, unique UUID string token
	    	 String rawToken = UUID.randomUUID().toString();
	    	  String hashedToken = DigestUtils.sha256Hex(rawToken); // or use passwordEncoder.encode + a lookup strategy
	    	  user.setResetToken(hashedToken);

	        // 2. Persist token with a 15-minute validity window
	        user.setResetToken(rawToken);
	        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
	        userRepository.save(user);

	        // 3. Construct a direct link matching your React application routes
	        String fullResetUrl = resetUrl + "/" +rawToken;
	       
	        log.info("Password reset token generated for user id: {}", user.getId());

	        try {
	            emailService.sendPasswordResetEmail(
	                    user.getEmail(),
	                    fullResetUrl
	            );
	        } catch (Exception e) {
	            log.error("Failed to send password reset email", e);
	        }
	    });
	}

	@Override
	@Transactional
	public void resetPassword(ResetPasswordRequestDTO request) {
		log.info("Processing password override sequence via cryptographic link token hash verification.");

		// 1. Basic validation check on payload
		if (request.getToken() == null || request.getToken().trim().isEmpty()) {
			throw new AuthenticationException("Missing reset token");
		}

		// 2. 🛠️ FIX: Apply matching SHA-256 hashing to the incoming user request token parameters
		String hashedIncomingToken = org.apache.commons.codec.digest.DigestUtils.sha256Hex(request.getToken().trim());

		// 3. 🛠️ FIX: Query the database ledger using the calculated hash signature
		User user = userRepository.findByResetToken(hashedIncomingToken)
				.orElseThrow(() -> new AuthenticationException("The reset link is invalid or has already been used."));

		// 4. Check expiration window rules
		if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
			throw new AuthenticationException("This reset link has expired. Please request a new one.");
		}

		// 4. Encrypt raw password entry and clear transient tokens
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		// 5. Encrypt raw password entry using BCrypt and clear transient tokens context safely
		user.setResetToken(null);
		user.setResetTokenExpiry(null);

		userRepository.save(user);
		
		log.info("Password override successful for user id: {}", user.getId());
	}

	

}
