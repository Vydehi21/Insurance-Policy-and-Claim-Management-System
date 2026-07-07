package com.monocept.project.controller;

import com.monocept.project.dto.ClaimStatusHistoryResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.ClaimStatusHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claim-history")
@RequiredArgsConstructor
@Tag(name = "Claim status history", description = "Operations for tracking and auditing historical changes in claim lifecycle states")
public class ClaimStatusHistoryController {

    private final ClaimStatusHistoryService claimStatusHistoryService;

    @GetMapping("/claim/{claimId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Get History By Claim ID", description = "Retrieves a paginated chronological log of all status transitions for a specific claim record")
    public ResponseEntity<PaginatedResponseDTO<ClaimStatusHistoryResponseDTO>>
    getHistoryByClaim(
            @PathVariable Long claimId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                claimStatusHistoryService.getHistoryByClaimId(
                        claimId,
                        page,
                        size,
                        sortBy,
                        direction,
                        userDetails.getUserId(),
                        userDetails.getRole()));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get History By User ID", description = "Retrieves workflow execution audits indicating changes initiated by an internal user account mapping")
    public ResponseEntity<PaginatedResponseDTO<ClaimStatusHistoryResponseDTO>>
    getHistoryByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimStatusHistoryService.getHistoryByUpdatedBy(
                        userId,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get History By Claim Status", description = "Filters target lifecycle logs containing matched transactional states")
    public ResponseEntity<PaginatedResponseDTO<ClaimStatusHistoryResponseDTO>>
    getHistoryByStatus(
            @PathVariable ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimStatusHistoryService.getHistoryByStatus(
                        status,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Filter History By Multiple Matrix Coordinates", description = "Runs explicit correlation lookups matching a target profile identifier, case context, and structural tracking state")
    public ResponseEntity<PaginatedResponseDTO<ClaimStatusHistoryResponseDTO>>
    getHistoryByClaimUserAndStatus(
            @RequestParam Long claimId,
            @RequestParam Long userId,
            @RequestParam ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimStatusHistoryService.getHistoryByClaimAndUserAndStatus(
                        claimId,
                        userId,
                        status,
                        page,
                        size,
                        sortBy,
                        direction));
    }
}