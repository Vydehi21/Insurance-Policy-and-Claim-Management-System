package com.monocept.project.service;

import java.time.LocalDate;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.monocept.project.dto.AgentPolicyIssueRequestDTO;
import com.monocept.project.dto.CustomerPolicyPurchaseRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyResponseDTO;
import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.Customer;
import com.monocept.project.model.Policy;
import com.monocept.project.model.PolicyPlan;
import com.monocept.project.repository.CustomerRepository;
import com.monocept.project.repository.PolicyPlanRepository;
import com.monocept.project.repository.PolicyRepository;
import com.monocept.project.service.PolicyService;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final PolicyPlanRepository policyPlanRepository;
    private final ModelMapper modelMapper;

    @Override
    public PolicyResponseDTO purchasePolicy(
            Long authenticatedUserId,
            CustomerPolicyPurchaseRequestDTO purchaseDTO) {

        Customer customer = customerRepository
                .findByUser_Id(authenticatedUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found"));

        PolicyPlan plan = policyPlanRepository
                .findById(purchaseDTO.getPlanId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plan not found"));

        policyRepository
                .findLatestPolicyByCustomerAndPlan(
                        customer.getId(),
                        plan.getId())
                .ifPresent(policy -> {

                    if (policy.getPolicyStatus() == PolicyStatus.PENDING_PAYMENT
                            || policy.getPolicyStatus() == PolicyStatus.ACTIVE) {

                        throw new BusinessRuleException(
                                "Policy already exists for this plan");
                    }
                });

        Policy policy = new Policy();

        policy.setPolicyNumber(
                "POL-" + UUID.randomUUID().toString().substring(0, 8));

        policy.setCustomer(customer);
        policy.setPolicyPlan(plan);

        policy.setStartDate(purchaseDTO.getStartDate());

        policy.setEndDate(
                purchaseDTO.getStartDate()
                        .plusYears(plan.getDuration()));

        policy.setPolicyStatus(PolicyStatus.PENDING_PAYMENT);

        Policy savedPolicy = policyRepository.save(policy);

        return mapToResponse(savedPolicy);
    }

    @Override
    public PolicyResponseDTO issuePolicy(
            AgentPolicyIssueRequestDTO issueDTO) {

        Customer customer = customerRepository
                .findById(issueDTO.getCustomerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found"));

        PolicyPlan plan = policyPlanRepository
                .findById(issueDTO.getPlanId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plan not found"));

        Policy policy = new Policy();

        policy.setPolicyNumber(
                "POL-" + UUID.randomUUID().toString().substring(0, 8));

        policy.setCustomer(customer);
        policy.setPolicyPlan(plan);

        policy.setStartDate(issueDTO.getStartDate());

        policy.setEndDate(
                issueDTO.getStartDate()
                        .plusYears(plan.getDuration()));

        policy.setPolicyStatus(PolicyStatus.ACTIVE);

        Policy savedPolicy = policyRepository.save(policy);

        return mapToResponse(savedPolicy);
    }

    @Override
    public PolicyResponseDTO getPolicyById(Long policyId) {

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Policy not found"));

        return mapToResponse(policy);
    }

    @Override
    public PolicyResponseDTO getPolicyByNumber(String policyNumber) {

        Policy policy = policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Policy not found"));

        return mapToResponse(policy);
    }

    @Override
    public PaginatedResponseDTO<PolicyResponseDTO> getAllPolicies(
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(
                page,
                size,
                sortBy,
                direction);

        Page<PolicyResponseDTO> result =
                policyRepository.findAll(pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByCustomerId(
            Long customerId,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(
                page,
                size,
                sortBy,
                direction);

        Page<PolicyResponseDTO> result =
                policyRepository
                        .findByCustomer_Id(customerId, pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByStatus(
            PolicyStatus status,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(
                page,
                size,
                sortBy,
                direction);

        Page<PolicyResponseDTO> result =
                policyRepository
                        .findByPolicyStatus(status, pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<PolicyResponseDTO>
            getPoliciesByCustomerAndStatus(
                    Long customerId,
                    PolicyStatus status,
                    int page,
                    int size,
                    String sortBy,
                    String direction) {

        Pageable pageable = createPageable(
                page,
                size,
                sortBy,
                direction);

        Page<PolicyResponseDTO> result =
                policyRepository
                        .findByCustomer_IdAndPolicyStatus(
                                customerId,
                                status,
                                pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<PolicyResponseDTO> searchPoliciesByNumber(
            String policyNumber,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = createPageable(
                page,
                size,
                sortBy,
                direction);

        Page<PolicyResponseDTO> result =
                policyRepository
                        .findByPolicyNumberContainingIgnoreCase(
                                policyNumber,
                                pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public void cancelPolicy(Long policyId) {

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Policy not found"));

        if (policy.getPolicyStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Policy already cancelled");
        }

        policy.setPolicyStatus(PolicyStatus.CANCELLED);

        policyRepository.save(policy);
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

    private PolicyResponseDTO mapToResponse(Policy policy) {

        PolicyResponseDTO dto = new PolicyResponseDTO();

        dto.setPolicyId(policy.getPolicyId());
        dto.setPolicyNumber(policy.getPolicyNumber());

        dto.setCustomerName(
                policy.getCustomer().getUser().getFullName());

        dto.setPlanName(
                policy.getPolicyPlan().getPlanName());

        dto.setProductType(
                policy.getPolicyPlan()
                        .getInsuranceProduct()
                        .getProductType());

        dto.setCoverageAmount(
                policy.getPolicyPlan()
                        .getCoverageAmount());

        dto.setPremiumAmount(
                policy.getPolicyPlan()
                        .getPremiumAmount());

        dto.setPremiumType(
                policy.getPolicyPlan()
                        .getPremiumType());

        dto.setStartDate(policy.getStartDate());
        dto.setEndDate(policy.getEndDate());

        dto.setPolicyStatus(policy.getPolicyStatus());

        dto.setTotalPremiumPaid(
                policy.getTotalPremiumPaid());

        return dto;
    }
}