package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.monocept.project.service.PolicyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173/")
@Tag(name = "Policies", description = "Operations for purchasing, issuing, tracking, and cancelling insurance policies")
public class PolicyController {

    private final PolicyService policyService;

	@PostMapping("/purchase")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Purchase Policy", description = "Allows an authenticated customer to buy a selected insurance plan")
	public ResponseEntity<PolicyResponseDTO> purchasePolicy(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody CustomerPolicyPurchaseRequestDTO dto) {

		Long userId = userDetails.getUserId();
		PolicyResponseDTO response = policyService.purchasePolicy(userId, dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
    
	@PostMapping("/issue")

	@PreAuthorize("hasAnyRole('ADMIN','AGENT')")
	@Operation(summary = "Issue Policy", description = "Allows an admin and agent to directly issue a policy package to a targeted consumer account")

	public ResponseEntity<PolicyResponseDTO> issuePolicy(
			@Valid @RequestBody AgentPolicyIssueRequestDTO dto) {

		PolicyResponseDTO response = policyService.issuePolicy(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping("/my")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<PaginatedResponseDTO<PolicyResponseDTO>> getMyPolicies(

	        @AuthenticationPrincipal CustomUserDetails userDetails,

	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(defaultValue = "id") String sortBy,
	        @RequestParam(defaultValue = "desc") String direction

	){

	    System.out.println("JWT USER ID = " + userDetails.getUserId());


	    return ResponseEntity.ok(

	        policyService.getMyPolicies(
	                userDetails.getUserId(),
	                page,
	                size,
	                sortBy,
	                direction
	        )

	    );
	}
	
//	@GetMapping("/my")
//	@PreAuthorize("hasRole('CUSTOMER')")
//	public ResponseEntity<?> getMyPolicies(
//	        @AuthenticationPrincipal CustomUserDetails userDetails
//	){
//
//	    return ResponseEntity.ok(
//	        policyService.getMyPolicies(
//	            userDetails.getUserId()
//	        )
//	    );
//	}

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CUSTOMER')")
    @Operation(summary = "Get Policy By ID", description = "Fetches complete entity schema fields for an active policy via primary record ID")
    public PolicyResponseDTO getPolicyById(
            @PathVariable Long id) {

        return policyService.getPolicyById(id);
    }

    @GetMapping("/number/{policyNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CUSTOMER')")
    @Operation(summary = "Get Policy By Number", description = "Retrieves specific insurance file parameters based on its alpha-numeric policy reference code")
    public PolicyResponseDTO getPolicyByNumber(
            @PathVariable String policyNumber) {

        return policyService.getPolicyByNumber(policyNumber);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Policies", description = "Returns a comprehensive paginated index payload tracking every system-wide policy record profile")
    public PaginatedResponseDTO<PolicyResponseDTO> getAllPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return policyService.getAllPolicies(
                page,
                size,
                sortBy,
                direction);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Get Policies By Customer", description = "Lists complete transactional assets linked directly to an established customer account context")
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
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Get Policies By Status", description = "Filters policy files based on active state parameters like active, lapsed, or pending")
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
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Get Policies By Customer And Status", description = "Correlates database instances filtering across a precise consumer and policy lifecycle filter")
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
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Search Policies By Number", description = "Performs text character matching queries to quickly isolate targeted policy reference points")
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
	@PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
	@Operation(summary = "Cancel Policy", description = "Transitions an operational contract straight into a cancelled classification status state")
	public ResponseEntity<String> cancelPolicy(
			@PathVariable Long policyId) {

		policyService.cancelPolicy(policyId);
		return ResponseEntity.ok("Policy cancelled successfully");
	}
	
	@GetMapping("/agent")
	@PreAuthorize("hasRole('AGENT')")
	@Operation(
	    summary = "Get Policies For Agent",
	    description = "Returns policies available for agent view"
	)
	public ResponseEntity<PaginatedResponseDTO<PolicyResponseDTO>>
	getAgentPolicies(

	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(defaultValue = "id") String sortBy,
	        @RequestParam(defaultValue = "desc") String direction

	){

	    return ResponseEntity.ok(

	        policyService.getAgentPolicies(
	                page,
	                size,
	                sortBy,
	                direction
	        )

	    );

	}
}
