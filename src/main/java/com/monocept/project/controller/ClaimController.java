package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import com.monocept.project.dto.ClaimFinalDecisionRequestDTO;
import com.monocept.project.dto.ClaimRequestDTO;
import com.monocept.project.dto.ClaimResponseDTO;
import com.monocept.project.dto.ClaimReviewRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.exception.AuthorizationException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.Claim;
import com.monocept.project.model.ClaimDocument;
import com.monocept.project.repository.ClaimDocumentRepository;
import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.ClaimService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Operations for creating, reviewing, processing, and querying insurance claims")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimDocumentRepository claimDocumentRepository;

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Raise Claim", description = "Initiates and creates a new insurance claim for the authenticated user")
	public ResponseEntity<ClaimResponseDTO> raiseClaim(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody ClaimRequestDTO dto
	) {
		ClaimResponseDTO response = claimService.raiseClaim(userDetails.getUserId(), dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{claimId}/review")
	@PreAuthorize("hasRole('AGENT')")
	@Operation(summary = "Review Claim", description = "Allows an internal agent to review a pending insurance claim")
	public ResponseEntity<ClaimResponseDTO> reviewClaim(
			@PathVariable Long claimId,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody ClaimReviewRequestDTO dto) {

		ClaimResponseDTO response = claimService.reviewClaim(
				claimId,
				userDetails.getUserId(),
				dto);

		return ResponseEntity.ok(response);
	}

	@PutMapping("/{claimId}/decision")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Process Final Decision", description = "Approve or reject an active claim profile based on full review metadata")
	public ResponseEntity<ClaimResponseDTO> processFinalDecision(
			@PathVariable Long claimId,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody ClaimFinalDecisionRequestDTO dto
	) {

		ClaimResponseDTO response = claimService.processFinalDecision(
				claimId,
				userDetails.getUserId(),
				dto);

		return ResponseEntity.ok(response);
	}

    @GetMapping("/{claimId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Get Claim By ID", description = "Fetches complete properties of a claim and applies concurrency locks if accessed by an internal agent")
    public ResponseEntity<ClaimResponseDTO> getClaimById(
            @PathVariable Long claimId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 🔐 If an agent requests this claim, route it through the lock acquisition engine
        if (userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"))) {
            
            return ResponseEntity.ok(
                    claimService.getClaimDetailsForReview(claimId, userDetails.getUserId())
            );
        }

        // Default read-only payload fallback for admins and customers
        return ResponseEntity.ok(
                claimService.getClaimById(claimId, userDetails.getUserId(), userDetails.getRole()));
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Claims", description = "Returns a paginated index payload tracking every system wide claim event log")
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    getAllClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimService.getAllClaims(
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get Claims By Customer ID", description = "Lists out complete insurance transaction instances unique to an established customer profile reference")
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    getClaimsByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimService.getClaimsByCustomerId(
                        customerId,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get Claims By Status", description = "Filters target records by standard state workflow values like pending, approved, or denied")
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    getClaimsByStatus(
            @PathVariable ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimService.getClaimsByStatus(
                        status,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Search Claims By Reference Number", description = "Provides targeted text string scanning to locate explicit item structures quickly")
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    searchClaims(
            @RequestParam String claimNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimService.searchClaimsByNumber(
                        claimNumber,
                        page,
                        size,
                        sortBy,
                        direction));
    }
    
    @GetMapping("/agent")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(
        summary = "Get Claims For Agent",
        description = "Returns claims assigned for agent review"
    )
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    getAgentClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {

        return ResponseEntity.ok(
        		 claimService.getAgentClaims(
        	                page,
        	                size,
        	                sortBy,
        	                direction
        	        )
        );
    }
    
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>> getMyClaims(

            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction

    ){

        return ResponseEntity.ok(

            claimService.getMyClaims(
                    userDetails.getUserId(),
                    page,
                    size,
                    sortBy,
                    direction
            )

        );
    }
    
	@PutMapping("/{claimId}/cancel")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Withdraw Claim", description = "Allows a customer to cancel their own pending claim request file safely")
	public ResponseEntity<Void> withdrawMyClaim(
			@PathVariable("claimId") Long claimId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		// 🛠️ FIXED: Passes BOTH matching arguments to satisfy the Service contract definitions
		claimService.withdrawClaimByCustomer(claimId, userDetails.getUserId());
		
		return ResponseEntity.ok().build();
	}


    
    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CUSTOMER')")
    public ResponseEntity<Void> viewDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {


        ClaimDocument document =
                claimDocumentRepository
                .findById(documentId)
                .orElseThrow(
                    () -> new ResourceNotFoundException(
                            "Document not found with id: " + documentId
                    )
                );

        // Enforces the same "own claims only" boundary as FR-CLM-005 for the
        // documents attached to those claims.
        Claim claim = document.getClaim();
        boolean isCustomer = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        if (isCustomer && !claim.getPolicy().getCustomer().getUser().getId().equals(userDetails.getUserId())) {
            throw new AuthorizationException("You are not authorized to view this document");
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.LOCATION,
                        document.getDocumentReference()
                )
                .build();
    }
}