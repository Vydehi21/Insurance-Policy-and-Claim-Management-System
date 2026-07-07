package com.monocept.project.service;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.UserRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.UserStatusUpdateRequestDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.User;
import com.monocept.project.repository.UserRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {

	private final UserRepository userRepository;
	private final ModelMapper modelMapper;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public UserResponseDTO createInternalStaff(UserRequestDTO userRequestDTO) {
	    log.info("Creating internal staff with email: {}", userRequestDTO.getEmail());

	    if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
	        log.warn("Internal Staff creation failed. Duplicate email: {}", userRequestDTO.getEmail());
	        throw new DuplicateResourceException("Email already exists: " + userRequestDTO.getEmail());
	    }

	    User user = modelMapper.map(userRequestDTO, User.class);

	    user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));

	    user.setRole(Role.INTERNAL_STAFF);
	    user.setActiveStatus(true);

	    User savedUser = userRepository.save(user);

	    log.info("Internal Staff created successfully with id: {}", savedUser.getId());

	    return modelMapper.map(savedUser, UserResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getUserById(Long userId) {
		log.info("Fetching user with id: {}", userId);

		User user = findUserById(userId);

		return modelMapper.map(user, UserResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getUserByEmail(String email) {
		log.info("Fetching user with email: {}", email);

		User user = userRepository.findByEmail(email).orElseThrow(() -> {
			log.warn("User not found with email: {}", email);
			return new ResourceNotFoundException("User not found with email: " + email);
		});

		return modelMapper.map(user, UserResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String direction) {
		log.info("Fetching all users");

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findAll(pageable);

		Page<UserResponseDTO> responsePage = users.map(user -> modelMapper.map(user, UserResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getUsersByRole(Role role, int page, int size, String sortBy,
			String direction) {
		log.info("Fetching users with role: {}", role);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByRole(role, pageable);

		Page<UserResponseDTO> responsePage = users.map(user -> modelMapper.map(user, UserResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getUsersByStatus(Boolean activeStatus, int page, int size,
			String sortBy, String direction) {
		log.info("Fetching users with status: {}", activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByActiveStatus(activeStatus, pageable);

		Page<UserResponseDTO> responsePage = users.map(user -> modelMapper.map(user, UserResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getUsersByRoleAndStatus(Role role, Boolean activeStatus, int page,
			int size, String sortBy, String direction) {
		log.info("Fetching users with role {} and status {}", role, activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByRoleAndActiveStatus(role, activeStatus, pageable);

		Page<UserResponseDTO> responsePage = users.map(user -> modelMapper.map(user, UserResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> searchUsersByName(String name, int page, int size, String sortBy,
			String direction) {
		log.info("Searching users by name: {}", name);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByFullNameContainingIgnoreCase(name, pageable);

		Page<UserResponseDTO> responsePage = users.map(user -> modelMapper.map(user, UserResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional
	public UserResponseDTO updateUserProfile(Long userId, UserRequestDTO userRequestDTO) {
		log.info("Updating user profile id: {}", userId);

		User user = findUserById(userId);

		if (!user.getEmail().equals(userRequestDTO.getEmail())
				&& userRepository.existsByEmail(userRequestDTO.getEmail())) {
			log.warn("Duplicate email during update: {}", userRequestDTO.getEmail());
			throw new DuplicateResourceException("Email already exists");
		}

		user.setFullName(userRequestDTO.getFullName());
		user.setEmail(userRequestDTO.getEmail());
		user.setMobileNumber(userRequestDTO.getMobileNumber());

		User updatedUser = userRepository.save(user);

		log.info("User updated successfully id: {}", updatedUser.getId());

		return modelMapper.map(updatedUser, UserResponseDTO.class);
	}

	@Override
	@Transactional
	public UserResponseDTO updateUserStatus(Long userId, UserStatusUpdateRequestDTO statusUpdateDTO) {
		log.info("Updating user status id: {}", userId);

		User user = findUserById(userId);

		user.setActiveStatus(statusUpdateDTO.getActiveStatus());

		User updatedUser = userRepository.save(user);
		
		log.info("User status updated successfully. id: {}, activeStatus: {}, remarks: {}",
			               updatedUser.getId(), updatedUser.getActiveStatus(), statusUpdateDTO.getRemarks());

		return modelMapper.map(updatedUser, UserResponseDTO.class);
	}

	private User findUserById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> {
			log.warn("User not found with id: {}", id);
			return new ResourceNotFoundException("User not found with id: " + id);
		});
	}
}