package com.monocept.project.service;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.CustomerRequestDTO;
import com.monocept.project.dto.CustomerResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.Customer;
import com.monocept.project.model.User;
import com.monocept.project.repository.CustomerRepository;
import com.monocept.project.repository.UserRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImplementation implements CustomerService {

	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final ModelMapper modelMapper;

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
	public CustomerResponseDTO updateCustomerProfile(Long customerId, CustomerRequestDTO customerRequestDTO) {
		log.info("Updating customer profile for customer id: {}", customerId);

		Customer customer = customerRepository.findById(customerId).orElseThrow(() -> {
			log.warn("Profile update failed. Customer not found for customer id: {}", customerId);
			return new ResourceNotFoundException("Customer not found");
		});

		customer.setDateOfBirth(customerRequestDTO.getDateOfBirth());

		customer.setAddress(customerRequestDTO.getAddress());

		customer.setCity(customerRequestDTO.getCity());

		customer.setState(customerRequestDTO.getState());

		customer.setPinCode(customerRequestDTO.getPinCode());

		customer.setNomineeName(customerRequestDTO.getNomineeName());

		customer.setNomineeRelation(customerRequestDTO.getNomineeRelation());

		Customer updatedCustomer = customerRepository.save(customer);

		log.info("Customer profile updated successfully with id: {}", updatedCustomer.getId());

		return modelMapper.map(updatedCustomer, CustomerResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<CustomerResponseDTO> searchCustomersByName(String name, int page, int size,
			String sortBy, String direction) {
		log.info("Searching customers by name: {}", name);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<Customer> customers = customerRepository.findByUserFullNameContainingIgnoreCase(name, pageable);

		Page<CustomerResponseDTO> responsePage = customers
				.map(customer -> modelMapper.map(customer, CustomerResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
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
}