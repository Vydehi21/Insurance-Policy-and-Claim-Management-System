package com.monocept.project.service;

import java.time.LocalDateTime;
import java.util.Optional;
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
import com.monocept.project.util.LogMaskUtil;
import com.monocept.project.util.PhoneNumberUtil;

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

	// Used only to give the "email not found" branch of login() a bcrypt
	// comparison to run against, so its response time matches the
	// "wrong password" branch instead of returning near-instantly. This is
	// just any valid bcrypt hash — it corresponds to no real password and
	// is never used to authenticate anything.
	private static final String DUMMY_PASSWORD_HASH =
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	@Value("${app.frontend.reset-url}")
	private String resetUrl;
	
	@Value("${jwt.expiration}")
	  private Long jwtExpiration;

	private final EmailOtpRepository emailOtpRepository;
	private final PhoneOtpRepository phoneOtpRepository;
	private final OtpService otpService;
	private final PendingUserRepository pendingUserRepository;

	// Matches PendingUser.expiryTime set on creation.
	private static final int PENDING_REGISTRATION_MINUTES = 10;
	
	@Override
	@Transactional
	public String registerCustomer(RegistrationRequestDTO registrationRequestDTO) {

		String email = registrationRequestDTO.getEmail();
		String mobile = PhoneNumberUtil.normalize(registrationRequestDTO.getMobileNumber());
		log.info("LOG-001 Registration started for {}", LogMaskUtil.email(email));

		if (userRepository.existsByEmail(email)) {
			throw new DuplicateResourceException("Email already exists: " + email);
		}

		// Check both "+91XXXXXXXXXX" and "XXXXXXXXXX" so numbers saved in the old
		// 10-digit format are still treated as duplicates.
		if (mobileAlreadyRegistered(mobile)) {
			throw new DuplicateResourceException("Phone number already exists: " + mobile);
		}

		// Abandoned registrations used to block the email/mobile forever (e.g. a
		// typo in the phone number). Anything past its expiry is cleared first.
		removeIfExpired(pendingUserRepository.findByEmail(email).orElse(null));
		removeIfExpired(pendingUserRepository.findByMobileNumber(mobile).orElse(null));

		if (pendingUserRepository.existsByEmail(email)) {
			throw new DuplicateResourceException("Registration already pending. Please verify OTP or resend OTP");
		}

		if (pendingUserRepository.existsByMobileNumber(mobile)) {
			throw new DuplicateResourceException("Registration already pending. Please verify OTP or resend OTP");
		}

		otpService.assertResendAllowed(email);

		// SMS first: if Twilio rejects the number, no email OTP has gone out yet.
		otpService.sendPhoneOtp(mobile);
		otpService.sendEmailOtp(email);

		PendingUser pendingUser = modelMapper.map(registrationRequestDTO, PendingUser.class);
		pendingUser.setMobileNumber(mobile);
		pendingUser.setPassword(passwordEncoder.encode(registrationRequestDTO.getPassword()));
		pendingUserRepository.save(pendingUser);

		return "OTP sent successfully";
	}

	@Override
	@Transactional
	public UserResponseDTO verifyRegister(VerifyRegistrationOtpDTO dto) {

	    log.info("Verifying registration OTPs for {}", LogMaskUtil.email(dto.getEmail()));

	    PendingUser pendingUser = pendingUserRepository.findByEmail(dto.getEmail())
	            .orElseThrow(() -> new ResourceNotFoundException("Registration expired. Please register again"));

	    if (pendingUser.getExpiryTime() != null && pendingUser.getExpiryTime().isBefore(LocalDateTime.now())) {
	        pendingUserRepository.delete(pendingUser);
	        throw new InvalidRequestException("Registration expired. Please register again");
	    }

	    boolean emailVerified = otpService.verifyEmailOtp(dto.getEmail(), dto.getEmailOtp());
	    if (!emailVerified) {
	        log.warn("Registration verification failed: invalid or expired email OTP");
	        throw new InvalidRequestException("Invalid or expired email OTP");
	    }

	    // The phone OTP is checked against the number saved at registration —
	    // never the number in this request. Using the request value let a caller
	    // verify a phone they control while the account kept an unverified one.
	    String registeredMobile = PhoneNumberUtil.normalize(pendingUser.getMobileNumber());
	    boolean phoneVerified = otpService.verifyPhoneOtp(registeredMobile, dto.getPhoneOtp());
	    if (!phoneVerified) {
	        log.warn("Registration verification failed: invalid phone OTP");
	        throw new InvalidRequestException("Invalid Phone OTP");
	    }

	    // Re-check in case the same email/phone was registered by someone else
	    // while this registration was pending.
	    if (userRepository.existsByEmail(pendingUser.getEmail())) {
	        throw new DuplicateResourceException("Email already exists: " + pendingUser.getEmail());
	    }
	    if (mobileAlreadyRegistered(registeredMobile)) {
	        throw new DuplicateResourceException("Phone number already exists: " + registeredMobile);
	    }

	    User user = modelMapper.map(pendingUser, User.class);

	    // CRITICAL: PendingUser's own primary key must never leak into the new User's primary key.
	    user.setId(null);
	    user.setMobileNumber(registeredMobile);
	    user.setRole(Role.CUSTOMER);
	    user.setActiveStatus(true);

	    User savedUser = userRepository.save(user);

	    emailOtpRepository.findByEmail(pendingUser.getEmail()).ifPresent(emailOtpRepository::delete);
	    phoneOtpRepository.findByPhone(registeredMobile).ifPresent(phoneOtpRepository::delete);
	    pendingUserRepository.delete(pendingUser);

	    log.info("LOG-001 Customer registration completed. User id: {}", savedUser.getId());

	    return modelMapper.map(savedUser, UserResponseDTO.class);
	}

	@Override
	@Transactional
	public String resendRegistrationOtp(ResendRegistrationOtpDTO dto) {

		PendingUser pendingUser = pendingUserRepository.findByEmail(dto.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException(
						"No pending registration found. Please register first"));

		if (pendingUser.getExpiryTime() != null && pendingUser.getExpiryTime().isBefore(LocalDateTime.now())) {
			pendingUserRepository.delete(pendingUser);
			throw new ResourceNotFoundException("Your registration has expired. Please register again");
		}

		otpService.assertResendAllowed(pendingUser.getEmail());

		otpService.sendPhoneOtp(pendingUser.getMobileNumber());
		otpService.sendEmailOtp(pendingUser.getEmail());

		// Keep an actively-verifying registration alive.
		pendingUser.setExpiryTime(LocalDateTime.now().plusMinutes(PENDING_REGISTRATION_MINUTES));
		pendingUserRepository.save(pendingUser);

		log.info("Registration OTP resent for {}", LogMaskUtil.email(dto.getEmail()));

		return "OTP resent successfully";
	}

	private boolean mobileAlreadyRegistered(String mobile) {
		return PhoneNumberUtil.variants(mobile).stream().anyMatch(userRepository::existsByMobileNumber);
	}

	private void removeIfExpired(PendingUser pendingUser) {
		if (pendingUser != null && pendingUser.getExpiryTime() != null
				&& pendingUser.getExpiryTime().isBefore(LocalDateTime.now())) {
			log.info("Removing expired pending registration id {}", pendingUser.getId());
			pendingUserRepository.delete(pendingUser);
			pendingUserRepository.flush();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
		log.info("Login attempt for {}", LogMaskUtil.email(loginRequestDTO.getEmail()));

		// SECURITY: findByEmail + a conditional bcrypt call creates a timing
		// side-channel — a non-existent email returns almost instantly (no
		// bcrypt), while a wrong password on a real account only fails after
		// a full (deliberately slow, ~50-100ms+) bcrypt comparison. Someone
		// timing responses could use that gap to enumerate which emails are
		// registered even though both cases show the same "Invalid
		// credentials" message. Always run a bcrypt comparison — against a
		// fixed dummy hash when there's no real user — so both paths take
		// comparable time.
		Optional<User> userOpt = userRepository.findByEmail(loginRequestDTO.getEmail());

		if (userOpt.isEmpty()) {
			passwordEncoder.matches(loginRequestDTO.getPassword(), DUMMY_PASSWORD_HASH);
			log.warn("LOG-003 Login failed. No account for {}", LogMaskUtil.email(loginRequestDTO.getEmail()));
			throw new AuthenticationException("Invalid credentials");
		}

		User user = userOpt.get();

		// FIXED: Safely verify hashed passwords instead of standard text equals
		// comparisons
				
		if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword()))  {
			log.warn("LOG-003 Login failed. Wrong password for user id: {}", user.getId());
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

		log.info("LOG-002 Login successful for user id: {}", user.getId());
		return response;
	}

	@Override
	@Transactional
	public void forgotPassword(ForgotPasswordRequestDTO request) {

	    // Look up the account, but don't throw if it's missing — silently do nothing
	    // for unregistered emails so the controller's generic response stays truthful
	    // for both cases (prevents user enumeration). ifPresentOrElse lets us still
	    // log the miss server-side (for abuse/enumeration monitoring) without
	    // changing anything about the client-facing response.
	    userRepository.findByEmail(request.getEmail()).ifPresentOrElse(user -> {

	    	// Deactivated accounts get the same silent no-op as unregistered
	    	// emails: no token, no email. A disabled account's credentials
	    	// shouldn't be changeable via self-service reset (it should go
	    	// through an admin reactivation flow), and staying silent here
	    	// avoids leaking active/inactive status via a different response.
	    	if (!Boolean.TRUE.equals(user.getActiveStatus())) {
	    		log.info("Password reset requested for a deactivated account id: {} — no token issued.", user.getId());
	    		return;
	    	}

	        // 1. Generate a secure, unique UUID string token
	    	 String rawToken = UUID.randomUUID().toString();
	    	  String hashedToken = DigestUtils.sha256Hex(rawToken);

	        // 2. Persist only the HASHED token, with a 15-minute validity window.
	        // The raw token is only ever put in the emailed link — never stored —
	        // so resetPassword() can hash the incoming token and match it here.
	        user.setResetToken(hashedToken);
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
	    }, () ->
	    	// No matching account. Deliberately not logging the raw email here —
	    	// it's attacker-controlled input, and echoing arbitrary user-supplied
	    	// strings straight into the log file is its own minor risk (log
	    	// injection / noise / accidental PII capture on typos of a real
	    	// address). Logging just the fact of a miss is enough to spot
	    	// enumeration attempts without that downside.
	    	log.info("Password reset requested for an email with no matching account — no token issued.")
	    );
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

		// 4b. Defense-in-depth: block redemption if the account was deactivated
		// after the token was issued but before it was used. Same generic
		// message as an invalid token is returned to the client, so this
		// doesn't leak account status — but log it plainly on the server
		// side so this specific case is distinguishable from an ordinary
		// invalid/reused/expired token when reviewing logs.
		if (!Boolean.TRUE.equals(user.getActiveStatus())) {
			log.warn("Password reset blocked: account id {} was deactivated after its reset token was issued.", user.getId());
			throw new AuthenticationException("The reset link is invalid or has already been used.");
		}

		// 4. Encrypt raw password entry and clear transient tokens
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		// 5. Encrypt raw password entry using BCrypt and clear transient tokens context safely
		user.setResetToken(null);
		user.setResetTokenExpiry(null);
		// Invalidates every JWT issued before this reset (see JwtAuthenticationFilter).
		user.setPasswordChangedAt(LocalDateTime.now());

		userRepository.save(user);
		
		log.info("Password override successful for user id: {}", user.getId());
	}

	

}