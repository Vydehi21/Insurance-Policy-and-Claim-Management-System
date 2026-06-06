package com.monocept.project.service;

import com.monocept.project.dto.PremiumPaymentRequestDTO;
import com.monocept.project.dto.PremiumPaymentResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.PaymentStatus;

public interface PremiumPaymentService {
    PremiumPaymentResponseDTO recordPayment(PremiumPaymentRequestDTO paymentRequestDTO);
    PremiumPaymentResponseDTO getPaymentById(Long paymentId);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByPolicyId(Long policyId, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByStatus(PaymentStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByPolicyAndStatus(Long policyId, PaymentStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PremiumPaymentResponseDTO> searchPaymentsByReference(String reference, int page, int size, String sortBy, String direction);
    
    //PremiumPaymentResponseDTO
   // getPaymentByReference(String reference);
}
