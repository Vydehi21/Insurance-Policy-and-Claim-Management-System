package com.monocept.project.service;

import com.monocept.project.dto.CustomerPolicyPurchaseRequestDTO;

import org.jspecify.annotations.Nullable;

import com.monocept.project.dto.InternalStaffPolicyIssueRequestDTO;
import com.monocept.project.dto.PolicyResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.PolicyStatus;

public interface PolicyService {
    PolicyResponseDTO purchasePolicy(Long authenticatedUserId, CustomerPolicyPurchaseRequestDTO purchaseDTO);
    PolicyResponseDTO issuePolicy(InternalStaffPolicyIssueRequestDTO issueDTO);
    PolicyResponseDTO getPolicyById(Long policyId, Long requesterUserId, String requesterRole);
    PolicyResponseDTO getPolicyByNumber(String policyNumber, Long requesterUserId, String requesterRole);
    PaginatedResponseDTO<PolicyResponseDTO> getAllPolicies(int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByCustomerId(Long customerId, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByStatus(PolicyStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByCustomerAndStatus(Long customerId, PolicyStatus status, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyResponseDTO> searchPoliciesByNumber(String policyNumber, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyResponseDTO> getMyPolicies(
            Long userId,
            int page,
            int size,
            String sortBy,
            String direction
    );
    void cancelPolicy(Long policyId);
    PaginatedResponseDTO<PolicyResponseDTO> getInternalStaffPolicies(
            int page,
            int size,
            String sortBy,
            String direction
    );
    
    //getPolicyByCustomerAndPolicyNumber(...)
}