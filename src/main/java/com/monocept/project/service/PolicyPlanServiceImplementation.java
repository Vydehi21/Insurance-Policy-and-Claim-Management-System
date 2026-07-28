package com.monocept.project.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyPlanRequestDTO;
import com.monocept.project.dto.PolicyPlanResponseDTO;
import com.monocept.project.dto.PremiumQuoteRequestDTO;
import com.monocept.project.dto.PremiumQuoteResponseDTO;
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.InsuranceProduct;
import com.monocept.project.model.PolicyPlan;
import com.monocept.project.repository.InsuranceProductRepository;
import com.monocept.project.repository.PolicyPlanRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyPlanServiceImplementation implements PolicyPlanService {

    private final PolicyPlanRepository policyPlanRepository;
    private final InsuranceProductRepository productRepository;
    private final PremiumCalculationService premiumCalculationService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public PolicyPlanResponseDTO createPlan(PolicyPlanRequestDTO planRequestDTO) {

        log.info("Creating policy plan: {}", planRequestDTO.getPlanName());

        InsuranceProduct product = findProductById(planRequestDTO.getProductId());

        if (Boolean.FALSE.equals(product.getActiveStatus())) {
            log.warn("Attempt to create a plan under inactive product id: {}", product.getId());
            throw new BusinessRuleException("Cannot create a plan under an inactive insurance product");
        }

        // Bean Validation (@AssertTrue on the DTO) already checked
        // max >= min, but a service-layer check costs nothing and guards
        // against this method ever being called with a hand-built DTO that
        // skipped validation.
        validateCoverageBounds(planRequestDTO.getMinCoverageAmount(), planRequestDTO.getMaxCoverageAmount());

        checkDuplicatePlanName(product.getId(), planRequestDTO.getPlanName(), null);
        checkDuplicatePlanTerms(product.getId(), planRequestDTO, null);

        PolicyPlan plan = new PolicyPlan();

        plan.setPlanName(planRequestDTO.getPlanName());
        plan.setMinCoverageAmount(planRequestDTO.getMinCoverageAmount());
        plan.setMaxCoverageAmount(planRequestDTO.getMaxCoverageAmount());
        plan.setRatePerUnit(planRequestDTO.getRatePerUnit());
        plan.setAnnualDiscountPercent(planRequestDTO.getAnnualDiscountPercent());
        plan.setOneTimeDiscountPercent(planRequestDTO.getOneTimeDiscountPercent());
        plan.setPremiumType(planRequestDTO.getPremiumType());
        plan.setDuration(planRequestDTO.getDuration());
        plan.setTermsAndConditions(planRequestDTO.getTermsAndConditions());
        plan.setActiveStatus(planRequestDTO.getActiveStatus());
        plan.setInsuranceProduct(product);

        PolicyPlan savedPlan = policyPlanRepository.save(plan);

        log.info("Policy plan created successfully id: {}", savedPlan.getId());

        return modelMapper.map(savedPlan, PolicyPlanResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyPlanResponseDTO getPlanById(Long planId) {
        log.info("Fetching policy plan with id: {}", planId);

        PolicyPlan plan = findPlanById(planId);

        return modelMapper.map(plan, PolicyPlanResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PolicyPlanResponseDTO> getAllPlans(int page, int size, String sortBy,
            String direction) {
        log.info("Fetching all policy plans");

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

        Page<PolicyPlan> plans = policyPlanRepository.findAll(pageable);

        Page<PolicyPlanResponseDTO> responsePage = plans
                .map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

        return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByStatus(Boolean activeStatus, int page, int size,
            String sortBy, String direction) {
        log.info("Fetching plans with status: {}", activeStatus);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

        Page<PolicyPlan> plans = policyPlanRepository.findByActiveStatus(activeStatus, pageable);

        Page<PolicyPlanResponseDTO> responsePage = plans
                .map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

        return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByProductId(Long productId, int page, int size,
            String sortBy, String direction) {
        log.info("Fetching plans for product id: {}", productId);

        findProductById(productId);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

        Page<PolicyPlan> plans = policyPlanRepository.findByInsuranceProductId(productId, pageable);

        Page<PolicyPlanResponseDTO> responsePage = plans
                .map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

        return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByProductIdAndStatus(Long productId,
            Boolean activeStatus, int page, int size, String sortBy, String direction) {
        log.info("Fetching plans for product id: {} and status: {}", productId, activeStatus);

        findProductById(productId);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

        Page<PolicyPlan> plans = policyPlanRepository.findByInsuranceProductIdAndActiveStatus(productId, activeStatus,
                pageable);

        Page<PolicyPlanResponseDTO> responsePage = plans
                .map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

        return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PolicyPlanResponseDTO> searchPlansByName(String name, int page, int size,
            String sortBy, String direction) {
        log.info("Searching plans by name: {}", name);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

        Page<PolicyPlan> plans = policyPlanRepository.findByPlanNameContainingIgnoreCase(name, pageable);

        Page<PolicyPlanResponseDTO> responsePage = plans
                .map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

        return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PolicyPlanResponseDTO> searchPlansByNameAndStatus(String name, Boolean activeStatus,
            int page, int size, String sortBy, String direction) {
        log.info("Searching plans by name: {} and status: {}", name, activeStatus);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

        Page<PolicyPlan> plans = policyPlanRepository
                .findByActiveStatusAndPlanNameContainingIgnoreCase(activeStatus, name, pageable);

        Page<PolicyPlanResponseDTO> responsePage = plans
                .map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

        return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
    }

    @Override
    @Transactional
    public PolicyPlanResponseDTO updatePlan(Long planId, PolicyPlanRequestDTO planRequestDTO) {
        log.info("Updating policy plan with id: {}", planId);

        PolicyPlan plan = findPlanById(planId);

        InsuranceProduct product = findProductById(planRequestDTO.getProductId());

        if (Boolean.FALSE.equals(product.getActiveStatus())) {
            log.warn("Attempt to move plan {} under inactive product id: {}", planId, product.getId());
            throw new BusinessRuleException("Cannot assign a plan to an inactive insurance product");
        }

        validateCoverageBounds(planRequestDTO.getMinCoverageAmount(), planRequestDTO.getMaxCoverageAmount());

        checkDuplicatePlanName(product.getId(), planRequestDTO.getPlanName(), planId);
        checkDuplicatePlanTerms(product.getId(), planRequestDTO, planId);

        plan.setPlanName(planRequestDTO.getPlanName());
        plan.setMinCoverageAmount(planRequestDTO.getMinCoverageAmount());
        plan.setMaxCoverageAmount(planRequestDTO.getMaxCoverageAmount());
        plan.setRatePerUnit(planRequestDTO.getRatePerUnit());
        plan.setAnnualDiscountPercent(planRequestDTO.getAnnualDiscountPercent());
        plan.setOneTimeDiscountPercent(planRequestDTO.getOneTimeDiscountPercent());
        plan.setPremiumType(planRequestDTO.getPremiumType());
        plan.setDuration(planRequestDTO.getDuration());
        plan.setTermsAndConditions(planRequestDTO.getTermsAndConditions());
        plan.setActiveStatus(planRequestDTO.getActiveStatus());
        plan.setInsuranceProduct(product);

        PolicyPlan updatedPlan = policyPlanRepository.save(plan);

        log.info("Policy plan updated successfully with id: {}", updatedPlan.getId());

        return modelMapper.map(updatedPlan, PolicyPlanResponseDTO.class);
    }

    @Override
    @Transactional
    public void deactivatePlan(Long planId) {
        log.info("Deactivating policy plan with id: {}", planId);

        PolicyPlan plan = findPlanById(planId);

        plan.setActiveStatus(false);

        policyPlanRepository.save(plan);

        log.info("Policy plan deactivated successfully with id: {}", planId);
    }

    @Override
    @Transactional
    public void activatePlan(Long planId) {

        PolicyPlan plan = findPlanById(planId);

        plan.setActiveStatus(true);

        policyPlanRepository.save(plan);

        log.info("Policy plan activated successfully with id: {}", planId);

    }

    @Override
    @Transactional(readOnly = true)
    public PremiumQuoteResponseDTO getPremiumQuote(Long planId, PremiumQuoteRequestDTO quoteRequestDTO) {
        log.info("Quoting premium for plan id: {}, coverage: {}, type: {}", planId,
                quoteRequestDTO.getCoverageAmount(), quoteRequestDTO.getPremiumType());

        PolicyPlan plan = findPlanById(planId);

        if (Boolean.FALSE.equals(plan.getActiveStatus())) {
            throw new BusinessRuleException("This plan is no longer available for purchase");
        }

        PremiumCalculationService.PremiumQuote quote = premiumCalculationService.calculateQuote(
                plan, quoteRequestDTO.getCoverageAmount(), quoteRequestDTO.getPremiumType());

        return new PremiumQuoteResponseDTO(
                quote.annualPremium(),
                quote.discountPercent(),
                quote.discountAmount(),
                quote.finalPremium());
    }

    private PolicyPlan findPlanById(Long id) {
        return policyPlanRepository.findById(id).orElseThrow(() -> {
            log.warn("Policy plan not found with id: {}", id);
            return new ResourceNotFoundException("Policy plan not found with id: " + id);
        });
    }

    private void validateCoverageBounds(BigDecimal minCoverage, BigDecimal maxCoverage) {
        if (minCoverage.compareTo(maxCoverage) > 0) {
            log.warn("Rejected plan: minCoverage {} is greater than maxCoverage {}", minCoverage, maxCoverage);
            throw new BusinessRuleException(
                    "Minimum coverage amount must be less than or equal to the maximum coverage amount");
        }
    }

    private void checkDuplicatePlanName(Long productId, String planName, Long excludePlanId) {
        Optional<PolicyPlan> existing = (excludePlanId == null)
                ? policyPlanRepository.findByInsuranceProduct_IdAndPlanNameIgnoreCase(productId, planName)
                : policyPlanRepository.findByInsuranceProduct_IdAndPlanNameIgnoreCaseAndIdNot(productId, planName,
                        excludePlanId);

        existing.ifPresent(plan -> {
            log.warn("Rejected plan: duplicate name under product id {}", productId);
            String message = String.format(
                    "A plan named '%s' already exists under this product (Plan ID: %d). Please choose a different name, or update the existing plan instead.",
                    plan.getPlanName(), plan.getId());
            throw new DuplicateResourceException(message);
        });
    }

    private void checkDuplicatePlanTerms(Long productId, PolicyPlanRequestDTO dto, Long excludePlanId) {

        Optional<PolicyPlan> existing = (excludePlanId == null)
                ? policyPlanRepository
                        .findByInsuranceProduct_IdAndMinCoverageAmountAndMaxCoverageAmountAndRatePerUnitAndAnnualDiscountPercentAndOneTimeDiscountPercentAndPremiumTypeAndDuration(
                                productId, dto.getMinCoverageAmount(), dto.getMaxCoverageAmount(),
                                dto.getRatePerUnit(), dto.getAnnualDiscountPercent(), dto.getOneTimeDiscountPercent(),
                                dto.getPremiumType(), dto.getDuration())
                : policyPlanRepository
                        .findByInsuranceProduct_IdAndMinCoverageAmountAndMaxCoverageAmountAndRatePerUnitAndAnnualDiscountPercentAndOneTimeDiscountPercentAndPremiumTypeAndDurationAndIdNot(
                                productId, dto.getMinCoverageAmount(), dto.getMaxCoverageAmount(),
                                dto.getRatePerUnit(), dto.getAnnualDiscountPercent(), dto.getOneTimeDiscountPercent(),
                                dto.getPremiumType(), dto.getDuration(), excludePlanId);

        existing.ifPresent(plan -> {
            log.warn("Rejected plan: identical terms to existing plan id {} under product id {}", plan.getId(),
                    productId);
            String message = String.format(
                    "An identical plan (same coverage range, rate, discounts, premium type, and duration) already exists under this product: '%s' (Plan ID: %d). Please update that plan instead of creating a duplicate.",
                    plan.getPlanName(), plan.getId());
            throw new DuplicateResourceException(message);
        });
    }

    private InsuranceProduct findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> {
            log.warn("Insurance product not found with id: {}", id);
            return new ResourceNotFoundException("Insurance product not found with id: " + id);
        });
    }
}