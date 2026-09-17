package com.monocept.project.service;

import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.UpdateStaffRequestDTO;
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
import com.monocept.project.util.LogMaskUtil;
import com.monocept.project.util.PaginationUtil;
import com.monocept.project.util.PhoneNumberUtil;

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
	    log.info("Creating internal staff with email: {}", LogMaskUtil.email(userRequestDTO.getEmail()));

	    if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
	        log.warn("Internal Staff creation failed. Duplicate email: {}", LogMaskUtil.email(userRequestDTO.getEmail()));
	        throw new DuplicateResourceException("Email already exists: " + userRequestDTO.getEmail());
	    }

	    if (mobileTaken(userRequestDTO.getMobileNumber(), null)) {
	        log.warn("Internal Staff creation failed. Mobile number already in use");
	        throw new DuplicateResourceException("Mobile number already exists: " + userRequestDTO.getMobileNumber());
	    }

	    // Built field by field on purpose — NOT with modelMapper.map(dto, User.class).
	    // ModelMapper's default (STANDARD) matching splits names into tokens, so the
	    // DTO's "assignedProductId" also matched the entity's "id". A staff member
	    // created with product #1 got user id 1, and save() then UPDATED the
	    // existing user 1 (the admin) instead of inserting a new user.
	    User user = new User();
	    user.setFullName(userRequestDTO.getFullName());
	    user.setEmail(userRequestDTO.getEmail());
	    user.setMobileNumber(PhoneNumberUtil.normalize(userRequestDTO.getMobileNumber()));

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
		log.info("Fetching user with email: {}", LogMaskUtil.email(email));

		User user = userRepository.findByEmail(email).orElseThrow(() -> {
			log.warn("User not found with email: {}", LogMaskUtil.email(email));
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

	@Transactional
	public UserResponseDTO updateInternalStaffProfile(Long userId, UpdateStaffRequestDTO requestDTO) {
		log.info("Processing internal staff profile modifications for target User ID: {}", userId);

		User user = findUserById(userId);

		// This endpoint is the "Update Agent Profile" screen. It used to accept
		// any user id, so the admin account or a customer could be edited here.
		if (user.getRole() != Role.INTERNAL_STAFF) {
			log.warn("Blocked staff-profile update on non internal-staff user id: {}", userId);
			throw new BusinessRuleException("Only internal staff profiles can be updated here");
		}

		String newEmail = requestDTO.getEmail().trim().toLowerCase();
		if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
			log.warn("Staff update blocked for user id {}: email already in use", userId);
			throw new DuplicateResourceException("This email address is already registered to an active profile.");
		}

		// Stored as +91XXXXXXXXXX like every other account (this used to strip
		// the +91), and checked for duplicates, which it wasn't before.
		String newMobile = PhoneNumberUtil.normalize(requestDTO.getMobileNumber());
		if (mobileTaken(newMobile, user)) {
			log.warn("Staff update blocked for user id {}: mobile number already in use", userId);
			throw new DuplicateResourceException("Mobile number already exists: " + newMobile);
		}

		user.setFullName(requestDTO.getFullName().trim());
		user.setEmail(newEmail);
		user.setMobileNumber(newMobile);

		if (requestDTO.getAssignedProductId() == null) {
			user.setAssignedProduct(null);
		} else {
			user.setAssignedProduct(findActiveProductOrThrow(requestDTO.getAssignedProductId()));
		}

		User updatedUser = userRepository.save(user);
		log.info("User database configurations updated successfully for ID: {}", updatedUser.getId());

		return toUserResponseDTO(updatedUser);
	}


	@Override
	@Transactional
	public UserResponseDTO updateUserStatus(Long userId, UserStatusUpdateRequestDTO statusUpdateDTO, Long actingUserId) {
		log.info("Updating user status id: {}", userId);

		User user = findUserById(userId);

		boolean deactivating = Boolean.FALSE.equals(statusUpdateDTO.getActiveStatus());

		// An admin deactivating their own account (or the last active admin)
		// would lock everyone out of administration permanently.
		if (deactivating && userId.equals(actingUserId)) {
			log.warn("Blocked self-deactivation attempt by user id: {}", actingUserId);
			throw new BusinessRuleException("You cannot deactivate your own account");
		}
		if (deactivating && user.getRole() == Role.ADMIN && Boolean.TRUE.equals(user.getActiveStatus())
				&& userRepository.countByRoleAndActiveStatus(Role.ADMIN, true) <= 1) {
			log.warn("Blocked deactivation of the last active admin, user id: {}", userId);
			throw new BusinessRuleException("At least one active admin account is required");
		}

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

	/** True when another account (other than {@code self}) already uses this phone in either stored format. */
	private boolean mobileTaken(String mobile, User self) {
		for (String variant : PhoneNumberUtil.variants(mobile)) {
			Optional<User> owner = userRepository.findByMobileNumber(variant);
			if (owner.isPresent() && (self == null || !owner.get().getId().equals(self.getId()))) {
				return true;
			}
		}
		return false;
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