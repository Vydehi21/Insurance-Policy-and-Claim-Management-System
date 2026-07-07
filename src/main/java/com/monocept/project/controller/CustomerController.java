package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monocept.project.dto.CustomerRequestDTO;
import com.monocept.project.dto.CustomerResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.repository.CustomerRepository;
import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173/")
@Tag(name = "customers", description = "Operations for managing insurance customer profile information")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;

	@PostMapping("/profile")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Create Customer Profile", description = "Initializes an operational customer entity record linked to the authenticated user")
	public ResponseEntity<CustomerResponseDTO> createProfile(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody CustomerRequestDTO dto) {

		CustomerResponseDTO response = customerService.createCustomerProfile(userDetails.getUserId(), dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{customerId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
	@Operation(summary = "Get Customer By ID", description = "Fetches core customer entity attributes matching the designated primary key identifier")
	public ResponseEntity<CustomerResponseDTO> getCustomerById(@PathVariable Long customerId) {
		return ResponseEntity.ok(customerService.getCustomerById(customerId));
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
	@Operation(summary = "Get Customer By User ID", description = "Retrieves relational customer structural configurations using the security user profile identity record link")
	public ResponseEntity<CustomerResponseDTO> getCustomerByUserId(@PathVariable Long userId) {
		return ResponseEntity.ok(customerService.getCustomerByUserId(userId));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','AGENT')")
	@Operation(summary = "Get All Customers", description = "Returns an indexed catalog tracking customer files registered system-wide")
	public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>> getAllCustomers(
			@RequestParam(defaultValue = "0") int page, 
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdDate") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return ResponseEntity.ok(customerService.getAllCustomers(page, size, sortBy, direction));
	}

	@GetMapping("/status/{activeStatus}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Get Customers By Status", description = "Filters target catalog profiles using categorical operational state parameters")
	public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>> getCustomersByStatus(
			@PathVariable Boolean activeStatus, 
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, 
			@RequestParam(defaultValue = "createdDate") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return ResponseEntity.ok(customerService.getCustomersByStatus(activeStatus, page, size, sortBy, direction));
	}

	@GetMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Search Customers", description = "Executes text-string criteria patterns across multiple customer parameters")
	public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>> searchCustomers(
			@RequestParam String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdDate") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return ResponseEntity.ok(
				customerService.searchCustomers(keyword, page, size, sortBy, direction)
		);
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Get Current Customer Profile", description = "Fetches active session identity context profile fields cleanly")
	public ResponseEntity<CustomerResponseDTO> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
		// 🛠️ FIXED: Removed structural duplicate unreachable return statements cleanly
		return ResponseEntity.ok(customerService.getCustomerByUserId(userDetails.getUserId()));
	}

    @GetMapping("/profile/exists")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Check Profile Existence", description = "Determines if the active user account has already built a matching customer record profile")
    public ResponseEntity<Boolean> profileExists(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // 🛠️ CLEAN: Communicates explicitly with the Service Layer wrapper!
        boolean exists = customerService.checkIfCustomerProfileExists(userDetails.getUserId());
        return ResponseEntity.ok(exists);
    }

   
	@PutMapping("/profile")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Update Personal Profile Details", description = "Modifies active session data details safely inside your persistent database storage records")
	public ResponseEntity<CustomerResponseDTO> updateMyProfile(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody CustomerRequestDTO dto) {

		return ResponseEntity.ok(customerService.updateCustomerProfile(userDetails.getUserId(), dto));
	}
}
