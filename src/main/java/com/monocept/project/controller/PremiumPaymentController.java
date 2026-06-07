package com.monocept.project.controller;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PremiumPaymentRequestDTO;
import com.monocept.project.dto.PremiumPaymentResponseDTO;
import com.monocept.project.enums.PaymentStatus;
import com.monocept.project.service.PremiumPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/premium-payments")
@RequiredArgsConstructor
public class PremiumPaymentController {

    private final PremiumPaymentService premiumPaymentService;

    @PostMapping
    public ResponseEntity<PremiumPaymentResponseDTO> recordPayment(
            @Valid @RequestBody PremiumPaymentRequestDTO requestDTO) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(premiumPaymentService.recordPayment(requestDTO));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PremiumPaymentResponseDTO> getPaymentById(
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(
                premiumPaymentService.getPaymentById(paymentId));
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<PaginatedResponseDTO<PremiumPaymentResponseDTO>>
    getPaymentsByPolicy(
            @PathVariable Long policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                premiumPaymentService.getPaymentsByPolicyId(
                        policyId,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PaginatedResponseDTO<PremiumPaymentResponseDTO>>
    getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                premiumPaymentService.getPaymentsByStatus(
                        status,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponseDTO<PremiumPaymentResponseDTO>>
    searchPayments(
            @RequestParam String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                premiumPaymentService.searchPaymentsByReference(
                        reference,
                        page,
                        size,
                        sortBy,
                        direction));
    }
}