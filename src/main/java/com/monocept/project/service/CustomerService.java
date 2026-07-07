package com.monocept.project.service;

import com.monocept.project.dto.CustomerRequestDTO;
import com.monocept.project.dto.CustomerResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;

public interface CustomerService {
    CustomerResponseDTO createCustomerProfile(Long userId, CustomerRequestDTO customerRequestDTO);
    CustomerResponseDTO getCustomerById(Long customerId);
    CustomerResponseDTO getCustomerByUserId(Long userId);
    PaginatedResponseDTO<CustomerResponseDTO> getAllCustomers(int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<CustomerResponseDTO> getCustomersByStatus(Boolean activeStatus, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<CustomerResponseDTO> searchCustomersByName(String name, int page, int size, String sortBy, String direction);
    CustomerResponseDTO updateCustomerProfile(Long customerId, CustomerRequestDTO customerRequestDTO);
	boolean profileExists(Long userId);
    
//    PaginatedResponseDTO<InsuranceProductResponseDTO>
//    getActiveProducts(...)
}
