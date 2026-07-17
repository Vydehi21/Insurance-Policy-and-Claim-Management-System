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
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.InsuranceProduct;
import com.monocept.project.model.User;
import com.monocept.project.repository.InsuranceProductRepository;
import com.monocept.project.repository.UserRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {

	private final UserRepository userRepository;
	private final InsuranceProductRepository insuranceProductRepository;
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

	    // §4.4 — optionally scope this internal-staff user to a product at
	    // creation time (PRD-BR-002: only active products are usable).
	    if (userRequestDTO.getAssignedProductId() != null) {
	        user.setAssignedProduct(findActiveProductOrThrow(userRequestDTO.getAssignedProductId()));
	    }

	    User savedUser = userRepository.save(user);

	    log.info("Internal Staff created successfully with id: {}", savedUser.getId());

	    return toUserResponseDTO(savedUser);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getUserById(Long userId) {
		log.info("Fetching user with id: {}", userId);

		User user = findUserById(userId);

		return toUserResponseDTO(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getUserByEmail(String email) {
		log.info("Fetching user with email: {}", email);

		User user = userRepository.findByEmail(email).orElseThrow(() -> {
			log.warn("User not found with email: {}", email);
			return new ResourceNotFoundException("User not found with email: " + email);
		});

		return toUserResponseDTO(user);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String direction) {
		log.info("Fetching all users");

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findAll(pageable);

		Page<UserResponseDTO> responsePage = users.map(this::toUserResponseDTO);

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getUsersByRole(Role role, int page, int size, String sortBy,
			String direction) {
		log.info("Fetching users with role: {}", role);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByRole(role, pageable);

		Page<UserResponseDTO> responsePage = users.map(this::toUserResponseDTO);

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getUsersByStatus(Boolean activeStatus, int page, int size,
			String sortBy, String direction) {
		log.info("Fetching users with status: {}", activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByActiveStatus(activeStatus, pageable);

		Page<UserResponseDTO> responsePage = users.map(this::toUserResponseDTO);

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> getUsersByRoleAndStatus(Role role, Boolean activeStatus, int page,
			int size, String sortBy, String direction) {
		log.info("Fetching users with role {} and status {}", role, activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByRoleAndActiveStatus(role, activeStatus, pageable);

		Page<UserResponseDTO> responsePage = users.map(this::toUserResponseDTO);

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<UserResponseDTO> searchUsersByName(String name, int page, int size, String sortBy,
			String direction) {
		log.info("Searching users by name: {}", name);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<User> users = userRepository.findByFullNameContainingIgnoreCase(name, pageable);

		Page<UserResponseDTO> responsePage = users.map(this::toUserResponseDTO);

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

		return toUserResponseDTO(updatedUser);
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

		return toUserResponseDTO(updatedUser);
	}

	@Override
	@Transactional
	public UserResponseDTO assignProductToUser(Long userId, Long productId) {
		log.info("Assigning product {} to user id: {}", productId, userId);

		User user = findUserById(userId);

		if (user.getRole() != Role.INTERNAL_STAFF) {
			log.warn("Attempted product assignment on non internal-staff user id: {}", userId);
			throw new BusinessRuleException("Only internal staff users can be assigned to a product");
		}

		if (productId == null) {
			user.setAssignedProduct(null);
		} else {
			user.setAssignedProduct(findActiveProductOrThrow(productId));
		}

		User updatedUser = userRepository.save(user);

		log.info("Product assignment updated for user id: {}, productId: {}", userId, productId);

		return toUserResponseDTO(updatedUser);
	}

	private InsuranceProduct findActiveProductOrThrow(Long productId) {
		InsuranceProduct product = insuranceProductRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

		// PRD-BR-002: only active products should be usable for new assignments.
		if (!Boolean.TRUE.equals(product.getActiveStatus())) {
			throw new BusinessRuleException("Cannot assign an inactive product: " + product.getProductName());
		}

		return product;
	}

	// Central conversion point for User -> UserResponseDTO so every list/detail
	// endpoint consistently surfaces the assigned-product fields (§4.4) instead
	// of each call site needing to remember to do it.
	private UserResponseDTO toUserResponseDTO(User user) {
		UserResponseDTO dto = modelMapper.map(user, UserResponseDTO.class);

		InsuranceProduct product = user.getAssignedProduct();
		if (product != null) {
			dto.setAssignedProductId(product.getId());
			dto.setAssignedProductName(product.getProductName());
		}

		return dto;
	}

	private User findUserById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> {
			log.warn("User not found with id: {}", id);
			return new ResourceNotFoundException("User not found with id: " + id);
		});
	}
}