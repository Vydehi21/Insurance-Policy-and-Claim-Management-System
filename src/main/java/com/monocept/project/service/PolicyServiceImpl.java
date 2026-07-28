package com.monocept.project.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.InternalStaffPolicyIssueRequestDTO;
import com.monocept.project.dto.CustomerPolicyPurchaseRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyResponseDTO;
import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.enums.PremiumType;
import com.monocept.project.exception.AuthorizationException;
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.InvalidRequestException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.Customer;
import com.monocept.project.model.Policy;
import com.monocept.project.model.PolicyPlan;
import com.monocept.project.repository.CustomerRepository;
import com.monocept.project.repository.PolicyPlanRepository;
import com.monocept.project.repository.PolicyRepository;
import com.monocept.project.repository.ClaimRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

	private final PolicyRepository policyRepository;
	private final CustomerRepository customerRepository;
	private final PolicyPlanRepository policyPlanRepository;
	private final ClaimRepository claimRepository;
	private final PremiumCalculationService premiumCalculationService;
	private final ModelMapper modelMapper;

	@Override
	@Transactional
	public PolicyResponseDTO purchasePolicy(Long authenticatedUserId, CustomerPolicyPurchaseRequestDTO purchaseDTO) {

		Customer customer = customerRepository.findByUser_Id(authenticatedUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

		PolicyPlan plan = policyPlanRepository.findById(purchaseDTO.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
		
		if (Boolean.FALSE.equals(plan.getActiveStatus())) {
			log.warn("Business rule violation. Attempt to purchase inactive plan: {}", plan.getId());
			
			throw new BusinessRuleException("This plan is no longer available for purchase");
			}

		policyRepository.findLatestPolicyByCustomerAndPlan(customer.getId(), plan.getId()).ifPresent(policy -> {

			if (policy.getPolicyStatus() == PolicyStatus.PENDING_PAYMENT
					|| policy.getPolicyStatus() == PolicyStatus.ACTIVE) {

				log.warn("Business rule violation. Customer {} already has policy for plan {}", customer.getId(),
						plan.getId());

				throw new BusinessRuleException("Policy already exists for this plan");
			}
		});

		LocalDate today = LocalDate.now();
		LocalDate requestedStartDate = purchaseDTO.getStartDate();

		if (requestedStartDate.isBefore(today)) {
			log.warn("Business rule violation. Attempt to purchase policy with backdated start date: {}",
					requestedStartDate);
			throw new BusinessRuleException(
					"Policy start date cannot be in the past. Please select today or a future date.");
		}

		if (requestedStartDate.isAfter(today.plusDays(30))) {
			log.warn("Business rule violation. Start date {} is beyond the allowed 30-day purchase window",
					requestedStartDate);
			throw new BusinessRuleException("Policy start date must be within 30 days of today.");
		}

		BigDecimal coverageAmount = purchaseDTO.getCoverageAmount();
		PremiumType premiumType = purchaseDTO.getPremiumType();
		validateCoverageWithinPlanBounds(plan, coverageAmount);

		BigDecimal premiumAmount = premiumCalculationService.calculatePremium(plan, coverageAmount, premiumType);

		Policy policy = new Policy();

		policy.setPolicyNumber("POL-" + UUID.randomUUID().toString().substring(0, 8));

		policy.setCustomer(customer);
		policy.setPolicyPlan(plan);

		policy.setCoverageAmount(coverageAmount);
		policy.setPremiumType(premiumType);
		policy.setPremiumAmount(premiumAmount);

		policy.setStartDate(requestedStartDate);

		policy.setEndDate(requestedStartDate.plusYears(plan.getDuration()));

		policy.setPolicyStatus(PolicyStatus.PENDING_PAYMENT);

		Policy savedPolicy = policyRepository.save(policy);

		log.info("LOG-006 Policy purchased. Policy number: {}", policy.getPolicyNumber());

		return mapToResponse(savedPolicy);
	}

	@Override
	@Transactional
	public PolicyResponseDTO issuePolicy(InternalStaffPolicyIssueRequestDTO issueDTO) {

		Customer customer = customerRepository.findById(issueDTO.getCustomerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

		PolicyPlan plan = policyPlanRepository.findById(issueDTO.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
		
		if (Boolean.FALSE.equals(plan.getActiveStatus())) {
			log.warn("Business rule violation. Attempt to purchase inactive plan: {}", plan.getId());
			
			throw new BusinessRuleException("This plan is no longer available for purchase");
			}

		if (issueDTO.getStartDate().isAfter(LocalDate.now().plusDays(30))) {
			log.warn("Business rule violation. Start date {} is beyond the allowed 30-day issuance window",
					issueDTO.getStartDate());
			throw new BusinessRuleException("Policy start date must be within 30 days of today.");
		}

		// Internal staff may omit coverage/frequency — default to the plan's
		// max coverage and the plan's own reference premiumType.
		BigDecimal coverageAmount = issueDTO.getCoverageAmount() != null
				? issueDTO.getCoverageAmount()
				: plan.getMaxCoverageAmount();
		PremiumType premiumType = issueDTO.getPremiumType() != null
				? issueDTO.getPremiumType()
				: plan.getPremiumType();

		validateCoverageWithinPlanBounds(plan, coverageAmount);

		BigDecimal premiumAmount = premiumCalculationService.calculatePremium(plan, coverageAmount, premiumType);

		Policy policy = new Policy();

		policy.setPolicyNumber("POL-" + UUID.randomUUID().toString().substring(0, 8));

		policy.setCustomer(customer);

		policy.setPolicyPlan(plan);

		policy.setCoverageAmount(coverageAmount);
		policy.setPremiumType(premiumType);
		policy.setPremiumAmount(premiumAmount);

		policy.setStartDate(issueDTO.getStartDate());

		policy.setEndDate(issueDTO.getStartDate().plusYears(plan.getDuration()));

		policy.setPolicyStatus(PolicyStatus.PENDING_PAYMENT);

		policy.setTotalPremiumPaid(BigDecimal.ZERO);

		Policy savedPolicy = policyRepository.save(policy);

		log.info("Policy issued {}", savedPolicy.getPolicyNumber());

		return mapToResponse(savedPolicy);

	}

	@Override
	public PolicyResponseDTO getPolicyById(Long policyId, Long requesterUserId, String requesterRole) {

		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

		enforcePolicyOwnership(policy, requesterUserId, requesterRole);

		return mapToResponse(policy);
	}

	@Override
	public PolicyResponseDTO getPolicyByNumber(String policyNumber, Long requesterUserId, String requesterRole) {

		Policy policy = policyRepository.findByPolicyNumber(policyNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

		enforcePolicyOwnership(policy, requesterUserId, requesterRole);

		return mapToResponse(policy);
	}

	@Override
	public PaginatedResponseDTO<PolicyResponseDTO> getAllPolicies(int page, int size, String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<PolicyResponseDTO> result = policyRepository.findAll(pageable).map(this::mapToResponse);

		return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
	}

	@Override
	public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByCustomerId(Long customerId, int page, int size,
			String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<PolicyResponseDTO> result = policyRepository

				.findByCustomer_Id(customerId, pageable)

				.map(this::mapToResponse);

		return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
	}

	@Override
	public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByStatus(PolicyStatus status, int page, int size,
			String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<PolicyResponseDTO> result = policyRepository.findByPolicyStatus(status, pageable).map(this::mapToResponse);

		return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
	}

	@Override
	public PaginatedResponseDTO<PolicyResponseDTO> getPoliciesByCustomerAndStatus(Long customerId, PolicyStatus status,
			int page, int size, String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<PolicyResponseDTO> result = policyRepository

				.findByCustomer_IdAndPolicyStatus(

						customerId, status, pageable)
				.map(this::mapToResponse);

		return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
	}

	@Override
	public PaginatedResponseDTO<PolicyResponseDTO> searchPoliciesByNumber(String policyNumber, int page, int size,
			String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<PolicyResponseDTO> result = policyRepository.findByPolicyNumberContainingIgnoreCase(policyNumber, pageable)
				.map(this::mapToResponse);

		return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
	}

	@Override
	@Transactional
	public void cancelPolicy(Long policyId) {

		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

		if (policy.getPolicyStatus() == PolicyStatus.CANCELLED) {

			log.warn("Business rule violation. Policy already cancelled: {}", policy.getPolicyNumber());
			throw new BusinessRuleException("Policy already cancelled");
		}
		
		policy.setPolicyStatus(PolicyStatus.CANCELLED);

		policyRepository.save(policy);
		log.info("Policy cancelled successfully. Policy number: {}", policy.getPolicyNumber());
	}

	private void validateCoverageWithinPlanBounds(PolicyPlan plan, BigDecimal coverageAmount) {
		if (coverageAmount == null) {
			throw new BusinessRuleException("Coverage amount is required");
		}

		// minCoverageAmount is now a required field on the plan, so no
		// default-value fallback is needed here anymore.
		BigDecimal minCoverage = plan.getMinCoverageAmount();
		BigDecimal maxCoverage = plan.getMaxCoverageAmount();

		if (coverageAmount.compareTo(minCoverage) < 0 || coverageAmount.compareTo(maxCoverage) > 0) {
			log.warn("Rejected purchase: coverage amount {} outside plan bounds [{}, {}]",
					coverageAmount, minCoverage, maxCoverage);
			throw new BusinessRuleException(
					"Coverage amount must be between " + minCoverage + " and " + maxCoverage + " for this plan");
		}
	}

	private PolicyResponseDTO mapToResponse(Policy policy) {

		PolicyResponseDTO dto = new PolicyResponseDTO();

		dto.setPolicyId(policy.getId());
		dto.setPolicyNumber(policy.getPolicyNumber());

		dto.setCustomerName(policy.getCustomer().getUser().getFullName());

		dto.setPlanName(policy.getPolicyPlan().getPlanName());

		dto.setProductType(policy.getPolicyPlan().getInsuranceProduct().getProductType());

		// CHANGED: these three now come from the policy (the customer's own
		// choices at purchase time), not the plan template.
		dto.setCoverageAmount(policy.getCoverageAmount());

		dto.setPremiumAmount(policy.getPremiumAmount());

		dto.setPremiumType(policy.getPremiumType());

		dto.setStartDate(policy.getStartDate());
		dto.setEndDate(policy.getEndDate());

		dto.setPolicyStatus(policy.getPolicyStatus());

		dto.setTotalPremiumPaid(policy.getTotalPremiumPaid());

		BigDecimal approvedClaimTotal = claimRepository.getApprovedClaimAmount(policy.getId());
		if (approvedClaimTotal == null) {
			approvedClaimTotal = BigDecimal.ZERO;
		}
		BigDecimal remainingCoverage = policy.getCoverageAmount().subtract(approvedClaimTotal);
		if (remainingCoverage.compareTo(BigDecimal.ZERO) < 0) {
			remainingCoverage = BigDecimal.ZERO;
		}
		dto.setRemainingCoverageAmount(remainingCoverage);

		return dto;
	}

	@Override
	public PaginatedResponseDTO<PolicyResponseDTO> getMyPolicies(Long userId, int page, int size, String sortBy,
			String direction) {

		Customer customer = customerRepository.findByUser_Id(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));


		return getPoliciesByCustomerId(customer.getId(), page, size, sortBy, direction);

	}

	@Override
	public PaginatedResponseDTO<PolicyResponseDTO> getInternalStaffPolicies(int page, int size, String sortBy,
			String direction) {

		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		Pageable pageable = PageRequest.of(page, size, sort);

		Page<PolicyResponseDTO> result = policyRepository.findAll(pageable).map(this::mapToResponse);

		return PaginationUtil.createPaginatedResponse(result, sortBy, direction);

	}
	
	@Scheduled(cron = "0 5 0 * * *")
	@Transactional
	public void expireOverduePolicies() {
	List<Policy> overdue = policyRepository
	               .findByPolicyStatusAndEndDateBefore(PolicyStatus.ACTIVE, LocalDate.now());
	
	        for (Policy policy : overdue) {
	            policy.setPolicyStatus(PolicyStatus.EXPIRED);
	        }
	        policyRepository.saveAll(overdue);
	
	        if (!overdue.isEmpty()) {
	            log.info("Policy expiry sweep completed. {} policies marked EXPIRED", overdue.size());
	        }

	        List<Policy> lapsed = policyRepository
	                .findByPolicyStatusAndStartDateBefore(PolicyStatus.PENDING_PAYMENT, LocalDate.now());

	        for (Policy policy : lapsed) {
	            policy.setPolicyStatus(PolicyStatus.CANCELLED);
	        }
	        policyRepository.saveAll(lapsed);

	        if (!lapsed.isEmpty()) {
	            log.info("Unpaid policy lapse sweep completed. {} policies marked CANCELLED", lapsed.size());
	        }
	    }

    private void enforcePolicyOwnership(Policy policy, Long requesterUserId, String requesterRole) {
        String normalizedRole = requesterRole != null ? requesterRole.toUpperCase() : "";

        if (normalizedRole.contains("CUSTOMER")) {
            if (policy.getCustomer() == null || policy.getCustomer().getUser() == null || 
                !policy.getCustomer().getUser().getId().equals(requesterUserId)) {
                
                log.warn("Blocked attempt by user {} to access another customer's policy: {}", 
                        requesterUserId, policy.getPolicyNumber());
                throw new AuthorizationException("You are not authorized to view this policy");
            }
        }
    }

}