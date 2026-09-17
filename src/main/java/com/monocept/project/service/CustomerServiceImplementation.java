package com.monocept.project.service;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.CustomerRequestDTO;
import com.monocept.project.dto.CustomerResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.Customer;
import com.monocept.project.model.EmailOtp;
import com.monocept.project.model.User;
import com.monocept.project.repository.CustomerRepository;
import com.monocept.project.repository.EmailOtpRepository;
import com.monocept.project.repository.UserRepository;
import com.monocept.project.util.PaginationUtil;
import com.monocept.project.util.PhoneNumberUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImplementation implements CustomerService {

	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final ModelMapper modelMapper;
	private final EmailOtpRepository emailOtpRepository;

	@Override
	@Transactional
	public CustomerResponseDTO createCustomerProfile(Long userId, CustomerRequestDTO customerRequestDTO) {
		log.info("Creating customer profile for user id: {}", userId);

		User user = findUserById(userId);

		if (customerRepository.existsByUser_Id(userId)) {
			log.warn("Customer profile creation failed. Profile already exists for user id: {}", userId);
			throw new DuplicateResourceException("Customer profile already exists");
		}

		Customer customer = modelMapper.map(customerRequestDTO, Customer.class);

		customer.setUser(user);

		Customer savedCustomer = customerRepository.save(customer);

		log.info("Customer profile created successfully with id: {}", savedCustomer.getId());

		return modelMapper.map(savedCustomer, CustomerResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerResponseDTO getCustomerById(Long customerId) {
		log.info("Fetching customer with id: {}", customerId);

		Customer customer = findCustomerById(customerId);

		return modelMapper.map(customer, CustomerResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerResponseDTO getCustomerByUserId(Long userId) {
		log.info("Fetching customer profile for user id: {}", userId);

		Customer customer =
		        customerRepository.findByUser_Id(userId)
		        .orElseThrow(() ->
		             new ResourceNotFoundException(
		                "Customer not found for user id: " + userId
		             )
		        );

		return modelMapper.map(customer, CustomerResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<CustomerResponseDTO> getAllCustomers(int page, int size, String sortBy,
			String direction) {
		log.info("Fetching all customers");

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<Customer> customers = customerRepository.findAll(pageable);

		Page<CustomerResponseDTO> responsePage = customers
				.map(customer -> modelMapper.map(customer, CustomerResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional
	public CustomerResponseDTO updateCustomerProfile(
	        Long userId,
	        CustomerRequestDTO dto) {


	    log.info("Updating profile for user id {}", userId);


	    Customer customer =
	            customerRepository.findByUser_Id(userId)
	            .orElseThrow(() ->
	                    new ResourceNotFoundException(
	                            "Customer profile not found"
	                    )
	            );


	    User user = customer.getUser();

	    // A field left out of the request keeps its current value.
	    String newEmail = dto.getEmail() != null ? dto.getEmail() : user.getEmail();
	    String newMobile = dto.getMobileNumber() != null
	            ? PhoneNumberUtil.normalize(dto.getMobileNumber())
	            : user.getMobileNumber();
	    String newFullName = dto.getFullName() != null ? dto.getFullName() : user.getFullName();
	    boolean emailChanged = !user.getEmail().equalsIgnoreCase(newEmail);
	    boolean mobileChanged = !PhoneNumberUtil.sameNumber(user.getMobileNumber(), newMobile);

	    if (emailChanged && userRepository.existsByEmail(newEmail)) {
	        log.warn("Customer profile update failed for user id {}: email already in use", userId);
	        throw new DuplicateResourceException("Email already exists: " + newEmail);
	    }

	    if (mobileChanged && PhoneNumberUtil.variants(newMobile).stream().anyMatch(userRepository::existsByMobileNumber)) {
	        log.warn("Customer profile update failed for user id {}: mobile number already in use", userId);
	        throw new DuplicateResourceException("Mobile number already exists: " + newMobile);
	    }

	    // The email is the login identity, so a change must be proven with the OTP
	    // sent to the new address (My Profile does this via /api/otp/email/*).
	    // Previously the API accepted the new email without checking the OTP.
	    EmailOtp verifiedOtp = null;
	    if (emailChanged) {
	        verifiedOtp = emailOtpRepository.findByEmail(newEmail)
	                .filter(EmailOtp::isVerified)
	                .filter(otp -> otp.getExpiryTime() != null && otp.getExpiryTime().isAfter(LocalDateTime.now()))
	                .orElseThrow(() -> new BusinessRuleException(
	                        "Please verify the new email address with the OTP before saving."));
	    }

	    user.setFullName(newFullName);
	    user.setEmail(newEmail);
	    user.setMobileNumber(newMobile);

	    userRepository.save(user);

	    if (verifiedOtp != null) {
	        // One-time use.
	        emailOtpRepository.delete(verifiedOtp);
	        log.info("Customer user id {} changed their login email", userId);
	    }



	    // update customer fields

	    customer.setDateOfBirth(dto.getDateOfBirth());

	    customer.setAddress(dto.getAddress());

	    customer.setCity(dto.getCity());

	    customer.setState(dto.getState());

	    customer.setPinCode(dto.getPinCode());

	    customer.setNomineeName(dto.getNomineeName());

	    customer.setNomineeRelation(dto.getNomineeRelation());



	    Customer saved =
	            customerRepository.save(customer);

	    log.info("Customer profile updated successfully for user id: {}", userId);

	    return modelMapper.map(
	            saved,
	            CustomerResponseDTO.class
	    );

	}
	
	@Override
	@Transactional(readOnly = true)
	public boolean checkIfCustomerProfileExists(Long userId) {
	    log.info("Checking if profile exists for user ID: {}", userId);
	    return customerRepository.existsByUser_Id(userId);
	}

	
	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<CustomerResponseDTO> searchCustomers(

	        String keyword,

	        int page,

	        int size,

	        String sortBy,

	        String direction) {

	    Pageable pageable =
	            PaginationUtil.createPageable(
	                    page,
	                    size,
	                    sortBy,
	                    direction
	            );

	    Page<Customer> customers =
	            customerRepository.searchCustomers(
	                    keyword,
	                    pageable
	            );

	    Page<CustomerResponseDTO> response =
	            customers.map(
	                    customer ->
	                            modelMapper.map(
	                                    customer,
	                                    CustomerResponseDTO.class
	                            )
	            );

	    return PaginationUtil.createPaginatedResponse(
	            response,
	            sortBy,
	            direction
	    );

	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<CustomerResponseDTO> getCustomersByStatus(Boolean activeStatus, int page, int size,
			String sortBy, String direction) {
		log.info("Fetching customers with active status: {}", activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<Customer> customers = customerRepository.findByUserActiveStatus(activeStatus, pageable);

		Page<CustomerResponseDTO> responsePage = customers
				.map(customer -> modelMapper.map(customer, CustomerResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	private Customer findCustomerById(Long id) {
		return customerRepository.findById(id).orElseThrow(() -> {
			log.warn("Customer not found with id: {}", id);
			return new ResourceNotFoundException("Customer not found with id: " + id);
		});
	}

	private User findUserById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> {
			log.warn("User not found with id: {}", id);
			return new ResourceNotFoundException("User not found with id: " + id);
		});
	}
	

	public CustomerResponseDTO getCustomerProfileByUserId(Long userId){

	    Customer customer =
	        customerRepository.findByUser_Id(userId)
	        .orElseThrow(
	            () -> new RuntimeException("Customer not found")
	        );

	    return modelMapper.map(customer, CustomerResponseDTO.class);
	}
	
	private CustomerResponseDTO convert(Customer customer){

		CustomerResponseDTO dto=new CustomerResponseDTO();

		dto.setCustomerId(customer.getId());

		dto.setDateOfBirth(customer.getDateOfBirth());
		dto.setAddress(customer.getAddress());
		dto.setCity(customer.getCity());
		dto.setState(customer.getState());
		dto.setPinCode(customer.getPinCode());

		dto.setNomineeName(customer.getNomineeName());
		dto.setNomineeRelation(customer.getNomineeRelation());

		dto.setActiveStatus(
		customer.getUser().getActiveStatus()
		);

		dto.setUserId(
		customer.getUser().getId()
		);


		dto.setFullName(
		customer.getUser().getFullName()
		);

		dto.setEmail(
		customer.getUser().getEmail()
		);

		dto.setMobileNumber(
		customer.getUser().getMobileNumber()
		);


		return dto;

		}
	
	@Override
	public boolean profileExists(Long userId) {
	    return customerRepository.existsByUser_Id(userId);
	}
}