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
import com.monocept.project.exception.InvalidRequestException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.ClaimStatusHistory;
import com.monocept.project.repository.ClaimStatusHistoryRepository;
import com.monocept.project.service.ClaimStatusHistoryService;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
                claimStatusHistoryRepository.findByClaim_Id(claimId, pageable);
        
        log.info(
                "Claim history fetched for claim id: {}",
                claimId
        );

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

                claimStatusHistoryRepository.findByUser_Id(userId, pageable);


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

                        .findByClaim_IdAndUser_IdAndNewStatus(

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


        if(page < 0) {

            log.warn(
                    "Invalid pagination request. Page: {}",
                    page
            );

            throw new InvalidRequestException(
                    "Page number cannot be negative");
        }


        if(size <= 0 || size > 100) {

            log.warn(
                    "Invalid pagination request. Size: {}",
                    size
            );

            throw new InvalidRequestException(
                    "Page size must be between 1 and 100");
        }


        Sort sort =
                direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();


        return PageRequest.of(
                page,
                size,
                sort);
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