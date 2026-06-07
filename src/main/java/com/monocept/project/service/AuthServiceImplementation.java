package com.monocept.project.service;

import com.monocept.project.exception.AuthenticationException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.LoginRequestDTO;
import com.monocept.project.dto.LoginResponseDTO;
import com.monocept.project.dto.RegistrationRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.model.User;
import com.monocept.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImplementation implements AuthService {
	
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
		if(!user.getPassword().equals(loginRequestDTO.getPassword())){
			log.warn("Login failed. Invalid password attempt for user id: {}", user.getId());
			throw new AuthenticationException("Invalid credentials");
		}
		
		if(!user.getActiveStatus()) {
			log.warn("Login failed. Inactive account access attempt. User id: {}", user.getId());
			throw new AuthenticationException("Inactive user account");
		}
		
		log.info("Login successful for user id: {}", user.getId());
		LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
		loginResponseDTO.setUserEmail(user.getEmail());
		loginResponseDTO.setUserRole(user.getRole());
		
		return loginResponseDTO;
	}
}
