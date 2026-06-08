package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monocept.project.dto.AgentPolicyIssueRequestDTO;
import com.monocept.project.dto.CustomerPolicyPurchaseRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyResponseDTO;
import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.security.CustomUserDetails;
//import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.PolicyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

//    @PostMapping("/purchase")
//    @PreAuthorize("hasRole('CUSTOMER')")
//    public PolicyResponseDTO purchasePolicy(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @Valid @RequestBody CustomerPolicyPurchaseRequestDTO purchaseDTO) {
//
//        return policyService.purchasePolicy(
//                userDetails.getUserId(),
//                purchaseDTO);
//    }

//    @PostMapping("/purchase")
//    public PolicyResponseDTO purchasePolicy(
//            @RequestParam Long userId,
//            @Valid @RequestBody CustomerPolicyPurchaseRequestDTO purchaseDTO) {
//
//        return policyService.purchasePolicy(
//                userId,
//                purchaseDTO);
//    }
	@PostMapping("/purchase")
	public ResponseEntity<PolicyResponseDTO> purchasePolicy(

			@AuthenticationPrincipal CustomUserDetails userDetails,

			@Valid @RequestBody CustomerPolicyPurchaseRequestDTO dto) {

		Long userId = userDetails.getUserId();

		PolicyResponseDTO response = policyService.purchasePolicy(userId, dto);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
    
	@PostMapping("/issue")
	public ResponseEntity<PolicyResponseDTO> issuePolicy(

			@Valid @RequestBody AgentPolicyIssueRequestDTO dto) {

		PolicyResponseDTO response = policyService.issuePolicy(dto);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

    @GetMapping("/{policyId}")
 //   @PreAuthorize("hasAnyRole('ADMIN','AGENT','CUSTOMER')")
    public PolicyResponseDTO getPolicyById(
            @PathVariable Long policyId) {

        return policyService.getPolicyById(policyId);
    }

    @GetMapping("/number/{policyNumber}")
 //   @PreAuthorize("hasAnyRole('ADMIN','AGENT','CUSTOMER')")
    public PolicyResponseDTO getPolicyByNumber(
            @PathVariable String policyNumber) {

        return policyService.getPolicyByNumber(policyNumber);
    }

    @GetMapping
  //  @PreAuthorize("hasRole('ADMIN')")
    public PaginatedResponseDTO<PolicyResponseDTO> getAllPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "policyId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return policyService.getAllPolicies(
                page,
                size,
                sortBy,
                direction);
    }

    @GetMapping("/customer/{customerId}")
 //   @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "policyId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return policyService.getPoliciesByCustomerId(
                customerId,
                page,
                size,
                sortBy,
                direction);
    }

    @GetMapping("/status/{status}")
 //   @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByStatus(
            @PathVariable PolicyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "policyId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return policyService.getPoliciesByStatus(
                status,
                page,
                size,
                sortBy,
                direction);
    }

    @GetMapping("/customer/{customerId}/status/{status}")
  //  @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByCustomerAndStatus(
            @PathVariable Long customerId,
            @PathVariable PolicyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "policyId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return policyService.getPoliciesByCustomerAndStatus(
                customerId,
                status,
                page,
                size,
                sortBy,
                direction);
    }

    @GetMapping("/search")
 //   @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public PaginatedResponseDTO<PolicyResponseDTO> searchPolicies(
            @RequestParam String policyNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "policyId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return policyService.searchPoliciesByNumber(
                policyNumber,
                page,
                size,
                sortBy,
                direction);
    }

	@PatchMapping("/{policyId}/cancel")
	public ResponseEntity<String> cancelPolicy(

			@PathVariable Long policyId) {

		policyService.cancelPolicy(policyId);

		return ResponseEntity.ok("Policy cancelled successfully");
	}
}