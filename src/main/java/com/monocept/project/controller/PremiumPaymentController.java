package com.monocept.project.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PremiumPaymentRequestDTO;
import com.monocept.project.dto.PremiumPaymentResponseDTO;
import com.monocept.project.enums.PaymentStatus;
import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.PremiumPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/premium-payments")
@RequiredArgsConstructor
@Tag(name = "Premium Payments", description = "Operations for processing, validating, and checking histories of premium policy payments")
public class PremiumPaymentController {

    private final PremiumPaymentService premiumPaymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    @Operation(summary = "Record Payment", description = "Registers and submits a new premium transaction receipt structure into the system ledger")
    public ResponseEntity<PremiumPaymentResponseDTO> recordPayment(
            @Valid @RequestBody PremiumPaymentRequestDTO requestDTO) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(premiumPaymentService.recordPayment(requestDTO));
    }
    

//    @GetMapping("/my")
//    @PreAuthorize("hasRole('CUSTOMER')")
//    public ResponseEntity<List<PremiumPaymentResponseDTO>> getMyPayments(
//            @AuthenticationPrincipal CustomUserDetails userDetails
//    ){
//
//        return ResponseEntity.ok(
//                premiumPaymentService.getPaymentsByCustomer(
//                        userDetails.getUserId()
//                )
//        );
//
//    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Get Payment By ID", description = "Fetches explicit transaction states matching the targeted unique ledger ID parameter")
    public ResponseEntity<PremiumPaymentResponseDTO> getPaymentById(
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(
                premiumPaymentService.getPaymentById(paymentId));
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Get Payments By Policy ID", description = "Extracts an indexed historical timeline of payments applied to a selected client policy file")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get Payments By Status", description = "Filters ledger audit instances using categorical payment states such as cleared, pending, or failed")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Search Payments By Reference", description = "Runs a quick text criteria tracking search to pinpoint exact system tracking vouchers")
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
