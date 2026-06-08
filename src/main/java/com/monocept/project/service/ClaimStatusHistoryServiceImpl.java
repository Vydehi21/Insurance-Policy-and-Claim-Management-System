package com.monocept.project.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.monocept.project.dto.ClaimStatusHistoryResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.ClaimStatusHistory;
import com.monocept.project.repository.ClaimStatusHistoryRepository;
import com.monocept.project.service.ClaimStatusHistoryService;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimStatusHistoryServiceImpl implements ClaimStatusHistoryService {

    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByClaimId(
            Long claimId,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);

        Page<ClaimStatusHistory> historyPage =
                claimStatusHistoryRepository.findByClaimId(claimId, pageable);

        if (historyPage.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No claim status history found for claim id: " + claimId);
        }

        return convertToPaginatedResponse(
                historyPage,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByUpdatedBy(
            Long userId,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);

        Page<ClaimStatusHistory> historyPage =
<<<<<<< HEAD
                claimStatusHistoryRepository.findByUser_Id(userId, pageable);
=======
                claimStatusHistoryRepository.findByUserId(userId, pageable);
>>>>>>> ea4edcbbf0ef94935e37b3ec82cecae4d98da256

        if (historyPage.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No claim status history found for user id: " + userId);
        }

        return convertToPaginatedResponse(
                historyPage,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByStatus(
            ClaimStatus status,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);

        Page<ClaimStatusHistory> historyPage =
                claimStatusHistoryRepository.findByNewStatus(status, pageable);

        if (historyPage.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No claim status history found for status: " + status);
        }

        return convertToPaginatedResponse(
                historyPage,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> getHistoryByClaimAndUserAndStatus(
            Long claimId,
            Long userId,
            ClaimStatus status,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);

        Page<ClaimStatusHistory> historyPage =
                claimStatusHistoryRepository
<<<<<<< HEAD
                        .findByClaim_ClaimIdAndUser_IdAndNewStatus(
=======
                        .findByClaimIdAndUserIdAndNewStatus(
>>>>>>> ea4edcbbf0ef94935e37b3ec82cecae4d98da256
                                claimId,
                                userId,
                                status,
                                pageable);

        if (historyPage.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No matching claim status history found");
        }

        return convertToPaginatedResponse(
                historyPage,
                sortBy,
                direction);
    }

    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return PageRequest.of(page, size, sort);
    }

    private PaginatedResponseDTO<ClaimStatusHistoryResponseDTO> convertToPaginatedResponse(
            Page<ClaimStatusHistory> historyPage,
            String sortBy,
            String direction) {

        List<ClaimStatusHistoryResponseDTO> responseList =
                historyPage.getContent()
                        .stream()
                        .map(this::convertToResponseDTO)
                        .collect(Collectors.toList());

        Page<ClaimStatusHistoryResponseDTO> dtoPage =
                historyPage.map(this::convertToResponseDTO);

        return PaginationUtil.createPaginatedResponse(
                dtoPage,
                sortBy,
                direction);
    }

    private ClaimStatusHistoryResponseDTO convertToResponseDTO(
            ClaimStatusHistory history) {

        ClaimStatusHistoryResponseDTO dto =
                modelMapper.map(
                        history,
                        ClaimStatusHistoryResponseDTO.class);

        dto.setClaimId(
                history.getClaim().getId());

        dto.setUpdatedByFullName(
                history.getUser().getFullName());

        return dto;
    }
}