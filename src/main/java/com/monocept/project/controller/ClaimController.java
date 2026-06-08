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

import com.monocept.project.dto.ClaimFinalDecisionRequestDTO;
import com.monocept.project.dto.ClaimRequestDTO;
import com.monocept.project.dto.ClaimResponseDTO;
import com.monocept.project.dto.ClaimReviewRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.ClaimService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

	@PostMapping
	public ResponseEntity<ClaimResponseDTO> raiseClaim(

			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody ClaimRequestDTO dto
	) {

		ClaimResponseDTO response = claimService.raiseClaim(userDetails.getUserId(), dto);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{claimId}/review")
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
    public ResponseEntity<ClaimResponseDTO> getClaimById(
            @PathVariable Long claimId) {

        return ResponseEntity.ok(
                claimService.getClaimById(claimId));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    getAllClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "claimId") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimService.getAllClaims(
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    getClaimsByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "claimId") String sortBy,
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
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    getClaimsByStatus(
            @PathVariable ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "claimId") String sortBy,
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
    public ResponseEntity<PaginatedResponseDTO<ClaimResponseDTO>>
    searchClaims(
            @RequestParam String claimNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "claimId") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimService.searchClaimsByNumber(
                        claimNumber,
                        page,
                        size,
                        sortBy,
                        direction));
    }
}