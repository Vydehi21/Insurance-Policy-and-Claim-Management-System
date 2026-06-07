package com.monocept.project.controller;

import com.monocept.project.dto.ClaimStatusHistoryResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.service.ClaimStatusHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claim-history")
@RequiredArgsConstructor
public class ClaimStatusHistoryController {

    private final ClaimStatusHistoryService claimStatusHistoryService;

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<PaginatedResponseDTO<ClaimStatusHistoryResponseDTO>>
    getHistoryByClaim(
            @PathVariable Long claimId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                claimStatusHistoryService.getHistoryByClaimId(
                        claimId,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/user/{userId}")
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