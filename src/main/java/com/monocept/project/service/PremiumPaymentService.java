package com.monocept.project.service;

import com.monocept.project.dto.PremiumPaymentRequestDTO;
import com.monocept.project.dto.PremiumPaymentResponseDTO;

import java.util.List;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.PaymentStatus;

public interface PremiumPaymentService {
    PremiumPaymentResponseDTO recordPayment(PremiumPaymentRequestDTO paymentRequestDTO, Long requesterUserId, String requesterRole);
    PremiumPaymentResponseDTO getPaymentById(Long paymentId, Long requesterUserId, String requesterRole);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByPolicyId(Long policyId, int page, int size, String sortBy, String direction, Long requesterUserId, String requesterRole);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByStatus(PaymentStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByPolicyAndStatus(Long policyId, PaymentStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> searchPaymentsByReference(String reference, int page, int size, String sortBy, String direction);
    public List<PremiumPaymentResponseDTO> getPaymentsByCustomer(Long userId);
    PaginatedResponseDTO<PremiumPaymentResponseDTO>
    getAllPayments(
            int page,
            int size,
            String sortBy,
            String direction
    );
}