package com.monocept.project.service;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.monocept.project.dto.ClaimFinalDecisionRequestDTO;
import com.monocept.project.dto.ClaimRequestDTO;
import com.monocept.project.dto.ClaimResponseDTO;
import com.monocept.project.dto.ClaimReviewRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;

public interface ClaimService {
    ClaimResponseDTO raiseClaim(Long authenticatedUserId, ClaimRequestDTO claimRequestDTO);
    ClaimResponseDTO reviewClaim(Long claimId, Long agentUserId, ClaimReviewRequestDTO reviewDTO);
    ClaimResponseDTO processFinalDecision(Long claimId, Long adminUserId, ClaimFinalDecisionRequestDTO decisionDTO);
    ClaimResponseDTO getClaimById(Long claimId);
    PaginatedResponseDTO<ClaimResponseDTO> getAllClaims(int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimResponseDTO> getClaimsByCustomerId(Long customerId, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimResponseDTO> getClaimsByStatus(ClaimStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimResponseDTO> getClaimsByCustomerAndStatus(Long customerId, ClaimStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimResponseDTO> searchClaimsByNumber(String claimNumber, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimResponseDTO> getMyClaims(
            Long userId,
            int page,
            int size,
            String sortBy,
            String direction
    );
	
    
//    PaginatedResponseDTO<ClaimResponseDTO>
//    getClaimsByPolicyId(
//        Long policyId,
//        int page,
//        int size,
//        String sortBy,
//        String direction);
}
