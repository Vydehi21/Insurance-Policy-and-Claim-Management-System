package com.monocept.project.service;

import com.monocept.project.dto.ClaimStatusHistoryResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;

public interface ClaimStatusHistoryService {
    PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByClaimId(Long claimId, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByUpdatedBy(Long userId, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByStatus(ClaimStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByClaimAndUserAndStatus(Long claimId, Long userId, ClaimStatus status, int page, int size, String sortBy, String direction);
}
