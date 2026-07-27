package com.monocept.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyPlanRequestDTO;
import com.monocept.project.dto.PolicyPlanResponseDTO;
import com.monocept.project.enums.PremiumType;
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

        // 🧮 AUTOMATED PREMIUM CALCULATION ALGORITHM
        BigDecimal coverage = planRequestDTO.getCoverageAmount();
        int years = planRequestDTO.getDuration();
        
        if (coverage == null || coverage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Coverage amount must be a positive value greater than zero");
        }
        if (coverage.remainder(BigDecimal.valueOf(50000)).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException("Coverage amount must be in multiples of 50,000");
        }
        if (years <= 0) {
            throw new BusinessRuleException("Plan duration term must be at least 1 year");
        }

        // Risk-rate compounding adjustments based on duration parameters
        double baselineRate = 0.05; // 5% base factor for short terms
        if (years >= 5) baselineRate = 0.042; // 4.2% factor for mid-length terms
        if (years >= 10) baselineRate = 0.035; // 3.5% factor for long-term investments

        // Formula: Premium = (Coverage / Years) * (1 + Baseline Rate)
        double annualBase = coverage.doubleValue() / years;
        double calculatedAnnualPremium = annualBase * (1.0 + baselineRate);

        // Adjust for Quarterly or Monthly payment intervals if requested in payload enums
        if (planRequestDTO.getPremiumType() == PremiumType.QUARTERLY) {
            calculatedAnnualPremium = calculatedAnnualPremium / 4.0;
        } else if (planRequestDTO.getPremiumType() == PremiumType.MONTHLY) {
            calculatedAnnualPremium = calculatedAnnualPremium / 12.0;
        }

        // Round cleanly to 2 decimal places matching financial account currencies
        BigDecimal automatedPremium = BigDecimal.valueOf(calculatedAnnualPremium)
                .divide(BigDecimal.valueOf(50000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(50000));

        // 🔒 Server overrides the field request values to prevent data tampering
        planRequestDTO.setPremiumAmount(automatedPremium);

        // Execute your multi-layered duplicate boundary check validations with the computed amount
        validateCoverageGreaterThanPremium(planRequestDTO.getCoverageAmount(), planRequestDTO.getPremiumAmount());

        checkDuplicatePlanName(product.getId(), planRequestDTO.getPlanName(), null);
        checkDuplicatePlanTerms(product.getId(), planRequestDTO.getCoverageAmount(),
                planRequestDTO.getPremiumAmount(), planRequestDTO.getPremiumType(),
                planRequestDTO.getDuration(), null);

        PolicyPlan plan = new PolicyPlan();
        plan.setPlanName(planRequestDTO.getPlanName());
        plan.setCoverageAmount(planRequestDTO.getCoverageAmount());
        plan.setPremiumAmount(planRequestDTO.getPremiumAmount()); // Injects automated premium
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
    @Transactional
    public PolicyPlanResponseDTO updatePlan(Long planId, PolicyPlanRequestDTO planRequestDTO) {

        log.info("Updating policy plan with id: {}", planId);

        PolicyPlan plan = findPlanById(planId);

        InsuranceProduct product = findProductById(planRequestDTO.getProductId());

        if (Boolean.FALSE.equals(product.getActiveStatus())) {
            log.warn("Attempt to move plan {} under inactive product id: {}", planId, product.getId());
            throw new BusinessRuleException("Cannot assign a plan to an inactive insurance product");
        }

        // 🧮 Recalculate premium automatically
        BigDecimal coverage = planRequestDTO.getCoverageAmount();
        int years = planRequestDTO.getDuration();

        if (coverage == null || coverage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Coverage amount must be a positive value greater than zero");
        }
        
        if (coverage.remainder(BigDecimal.valueOf(50000)).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException("Coverage amount must be in multiples of 50,000");
        }

        if (years <= 0) {
            throw new BusinessRuleException("Plan duration term must be at least 1 year");
        }

        double baselineRate = 0.05;

        if (years >= 5)
            baselineRate = 0.042;

        if (years >= 10)
            baselineRate = 0.035;

        double annualBase = coverage.doubleValue() / years;
        double calculatedAnnualPremium = annualBase * (1.0 + baselineRate);

        if (planRequestDTO.getPremiumType() == PremiumType.QUARTERLY) {
            calculatedAnnualPremium /= 4.0;
        } else if (planRequestDTO.getPremiumType() == PremiumType.MONTHLY) {
            calculatedAnnualPremium /= 12.0;
        }

        BigDecimal automatedPremium = BigDecimal.valueOf(calculatedAnnualPremium)
                .divide(BigDecimal.valueOf(50000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(50000));
        // Ignore premium sent from frontend
        planRequestDTO.setPremiumAmount(automatedPremium);

        validateCoverageGreaterThanPremium(
                planRequestDTO.getCoverageAmount(),
                planRequestDTO.getPremiumAmount());

        checkDuplicatePlanName(product.getId(), planRequestDTO.getPlanName(), planId);

        checkDuplicatePlanTerms(
                product.getId(),
                planRequestDTO.getCoverageAmount(),
                planRequestDTO.getPremiumAmount(),
                planRequestDTO.getPremiumType(),
                planRequestDTO.getDuration(),
                planId);

        plan.setPlanName(planRequestDTO.getPlanName());
        plan.setCoverageAmount(planRequestDTO.getCoverageAmount());
        plan.setPremiumAmount(planRequestDTO.getPremiumAmount());
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

    private PolicyPlan findPlanById(Long id) {
        return policyPlanRepository.findById(id).orElseThrow(() -> {
            log.warn("Policy plan not found with id: {}", id);
            return new ResourceNotFoundException("Policy plan not found with id: " + id);
        });
    }

    private void validateCoverageGreaterThanPremium(BigDecimal coverageAmount, BigDecimal premiumAmount) {
        if (coverageAmount.compareTo(premiumAmount) <= 0) {
            log.warn("Rejected plan: coverage amount {} is not greater than premium amount {}",
                    coverageAmount, premiumAmount);
            throw new BusinessRuleException("Coverage amount must be greater than premium amount");
        }
    }

    /**
     * Enforces that a plan name is unique within its product (case-insensitive).
     * excludePlanId is null on create, and set to the plan's own id on update
     * (so a plan isn't flagged as a duplicate of itself).
     */
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

    /**
     * Enforces that coverage amount, premium amount, premium type, and duration
     * together aren't identical to another plan under the same product. A change
     * in premium type (e.g. one-time vs annual) is treated as a genuinely
     * different plan, not a duplicate.
     */
    private void checkDuplicatePlanTerms(Long productId, BigDecimal coverageAmount, BigDecimal premiumAmount,
            PremiumType premiumType, Integer duration, Long excludePlanId) {

        Optional<PolicyPlan> existing = (excludePlanId == null)
                ? policyPlanRepository.findByInsuranceProduct_IdAndCoverageAmountAndPremiumAmountAndPremiumTypeAndDuration(
                        productId, coverageAmount, premiumAmount, premiumType, duration)
                : policyPlanRepository
                        .findByInsuranceProduct_IdAndCoverageAmountAndPremiumAmountAndPremiumTypeAndDurationAndIdNot(
                                productId, coverageAmount, premiumAmount, premiumType, duration, excludePlanId);

        existing.ifPresent(plan -> {
            log.warn("Rejected plan: identical terms to existing plan id {} under product id {}", plan.getId(),
                    productId);
            String message = String.format(
                    "An identical plan (same coverage amount, premium amount, premium type, and duration) already exists under this product: '%s' (Plan ID: %d). Please update that plan instead of creating a duplicate.",
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