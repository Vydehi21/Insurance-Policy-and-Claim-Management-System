package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<CustomerResponseDTO> createProfile(

            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CustomerRequestDTO dto

    ) {
        CustomerResponseDTO response =
                customerService.createCustomerProfile(
                        userDetails.getUserId(),
                        dto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDTO> getCustomerById(

            @PathVariable Long customerId) {

        return ResponseEntity.ok(

                customerService.getCustomerById(
                        customerId
                )
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomerResponseDTO> getCustomerByUserId(

            @PathVariable Long userId) {


        return ResponseEntity.ok(

                customerService.getCustomerByUserId(
                        userId
                )
        );
    }

    @GetMapping
    public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>>
    getAllCustomers(

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(

                customerService.getAllCustomers(
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @GetMapping("/status/{activeStatus}")
    public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>>
    getCustomersByStatus(

            @PathVariable Boolean activeStatus,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {

        return ResponseEntity.ok(

                customerService.getCustomersByStatus(
                        activeStatus,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponseDTO<CustomerResponseDTO>>
    searchCustomers(

            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
        		
                customerService.searchCustomersByName(
                        name,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDTO> updateCustomerProfile(

            @PathVariable Long customerId,
            @Valid
            @RequestBody CustomerRequestDTO customerRequestDTO) {

        return ResponseEntity.ok(

                customerService.updateCustomerProfile(
                        customerId,
                        customerRequestDTO
                )
        );
    }
    
}