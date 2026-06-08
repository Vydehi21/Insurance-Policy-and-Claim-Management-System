package com.monocept.project.service;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.LoginRequestDTO;
import com.monocept.project.dto.LoginResponseDTO;
import com.monocept.project.dto.RegistrationRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.exception.AuthenticationException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.model.User;
import com.monocept.project.repository.UserRepository;
import com.monocept.project.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImplementation implements AuthService {
	
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	
	private final UserRepository userRepository;
	private final ModelMapper modelMapper;
	
	@Override
	@Transactional
	public UserResponseDTO registerCustomer(RegistrationRequestDTO registrationRequestDTO) {
		log.info("Registering customer: {}", registrationRequestDTO.getEmail());
		if(userRepository.existsByEmail(registrationRequestDTO.getEmail())) {
			throw new DuplicateResourceException("Email already exists: " + registrationRequestDTO.getEmail());
		}
		
		User user = modelMapper.map(registrationRequestDTO, User.class);
		user.setPassword(
		        passwordEncoder.encode(registrationRequestDTO.getPassword())
		);
		user.setRole(Role.CUSTOMER);
		user.setActiveStatus(true);
		
		User savedUser = userRepository.save(user);
		
		log.info("User registered with id: {}", savedUser.getId());
		return modelMapper.map(savedUser, UserResponseDTO.class);
	}
	
	@Override
	@Transactional(readOnly = true)
	public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
		log.info("Login attempt: {}", loginRequestDTO.getEmail());
		
		User user = userRepository.findByEmail(loginRequestDTO.getEmail())
					.orElseThrow(()->{
						log.warn("Login failed. Email not found: {}", loginRequestDTO.getEmail());
						return new AuthenticationException("Invalid credentials");
					});
		if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {

			log.warn("Wrong password attempt for email: {}", user.getEmail());
			throw new AuthenticationException("Invalid credentials");
		}
		
		if(!user.getActiveStatus()) {
			log.warn("Login failed. Inactive account access attempt. User id: {}", user.getId());
			throw new AuthenticationException("Inactive user account");
		}
		
		String token = jwtService.generateToken(user);
		LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
		loginResponseDTO.setJwtToken(token);
		loginResponseDTO.setTokenType("Bearer");
		loginResponseDTO.setUserEmail(user.getEmail());
		loginResponseDTO.setUserRole(user.getRole());
		
		log.info("Login successful for user id: {}", user.getId());
		
		return loginResponseDTO;
	}
}
