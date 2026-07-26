package com.monocept.project.repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.model.PolicyPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyPlanRepository extends JpaRepository<PolicyPlan, Long> {

    Page<PolicyPlan> findByInsuranceProductId(Long productId, Pageable pageable);
    Page<PolicyPlan> findByActiveStatus(Boolean activeStatus, Pageable pageable);
    Page<PolicyPlan> findByInsuranceProductIdAndActiveStatus(Long productId, Boolean activeStatus, Pageable pageable);

    Page<PolicyPlan> findByPlanNameContainingIgnoreCase(String planName, Pageable pageable);
    Page<PolicyPlan> findByActiveStatusAndPlanNameContainingIgnoreCase(Boolean activeStatus, String planName, Pageable pageable);

    Optional<PolicyPlan> findByInsuranceProduct_IdAndPlanNameIgnoreCase(Long productId, String planName);

    Optional<PolicyPlan> findByInsuranceProduct_IdAndPlanNameIgnoreCaseAndIdNot(
            Long productId, String planName, Long id);

    Optional<PolicyPlan> findByInsuranceProduct_IdAndCoverageAmountAndPremiumAmountAndPremiumTypeAndDuration(
            Long productId, BigDecimal coverageAmount, BigDecimal premiumAmount,
            PremiumType premiumType, Integer duration);

    Optional<PolicyPlan> findByInsuranceProduct_IdAndCoverageAmountAndPremiumAmountAndPremiumTypeAndDurationAndIdNot(
            Long productId, BigDecimal coverageAmount, BigDecimal premiumAmount,
            PremiumType premiumType, Integer duration, Long id);
}