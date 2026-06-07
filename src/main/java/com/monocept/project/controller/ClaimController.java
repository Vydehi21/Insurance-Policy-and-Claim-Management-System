package com.monocept.project.controller;

import com.monocept.project.dto.*;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.service.ClaimService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponseDTO> raiseClaim(
            @RequestParam Long userId,
            @Valid @RequestBody ClaimRequestDTO requestDTO) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimService.raiseClaim(userId, requestDTO));
    }

    @PutMapping("/{claimId}/review")
    public ResponseEntity<ClaimResponseDTO> reviewClaim(
            @PathVariable Long claimId,
            @RequestParam Long agentId,
            @Valid @RequestBody ClaimReviewRequestDTO requestDTO) {

        return ResponseEntity.ok(
                claimService.reviewClaim(
                        claimId,
                        agentId,
                        requestDTO));
    }

    @PutMapping("/{claimId}/decision")
    public ResponseEntity<ClaimResponseDTO> processFinalDecision(
            @PathVariable Long claimId,
            @RequestParam Long adminId,
            @Valid @RequestBody ClaimFinalDecisionRequestDTO requestDTO) {

        return ResponseEntity.ok(
                claimService.processFinalDecision(
                        claimId,
                        adminId,
                        requestDTO));
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