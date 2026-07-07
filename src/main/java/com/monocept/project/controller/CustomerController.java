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

import com.monocept.project.dto.ClaimResponseDTO;
import com.monocept.project.dto.CustomerRequestDTO;
import com.monocept.project.dto.CustomerResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
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

	@PostMapping("/profile")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<CustomerResponseDTO> createProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
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
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdDate") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return ResponseEntity.ok(customerService.getAllCustomers(page, size, sortBy, direction));
	}

	@GetMapping("/status/{activeStatus}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Get Customers By Status", description = "Filters target catalog profiles using categorical operational state parameters")
	public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>> getCustomersByStatus(
			@PathVariable Boolean activeStatus, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdDate") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return ResponseEntity.ok(customerService.getCustomersByStatus(activeStatus, page, size, sortBy, direction));
	}

//    @GetMapping("/search")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Search Customers By Name", description = "Executes text-string criteria patterns to match name components in target logs")
//    public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>>
//    searchCustomers(
//            @RequestParam String name,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "createdDate") String sortBy,
//            @RequestParam(defaultValue = "desc") String direction) {
//
//        return ResponseEntity.ok(
//                customerService.searchCustomersByName(
//                        name,
//                        page,
//                        size,
//                        sortBy,
//                        direction
//                )
//        );
//    }

	@GetMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>> searchCustomers(

			@RequestParam String keyword,

			@RequestParam(defaultValue = "0") int page,

			@RequestParam(defaultValue = "10") int size,

			@RequestParam(defaultValue = "createdDate") String sortBy,

			@RequestParam(defaultValue = "desc") String direction) {

		return ResponseEntity.ok(

				customerService.searchCustomers(

						keyword,

						page,

						size,

						sortBy,

						direction

				)

		);

	}

//    @PutMapping("/{customerId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
//    @Operation(summary = "Update Customer Profile", description = "Modifies existing customer registration data records based on the payload data structures")
//    public ResponseEntity<CustomerResponseDTO> updateCustomerProfile(
//            @PathVariable Long customerId,
//            @Valid @RequestBody CustomerRequestDTO customerRequestDTO) {
//
//        return ResponseEntity.ok(
//                customerService.updateCustomerProfile(
//                        customerId,
//                        customerRequestDTO
//                )
//        );
//    }

	@GetMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<CustomerResponseDTO> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {

		return ResponseEntity.ok(customerService.getCustomerByUserId(userDetails.getUserId()));

	}

	@PutMapping("/profile")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<CustomerResponseDTO> updateMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody CustomerRequestDTO dto) {

		return ResponseEntity.ok(customerService.updateCustomerProfile(userDetails.getUserId(), dto));
	}

}
