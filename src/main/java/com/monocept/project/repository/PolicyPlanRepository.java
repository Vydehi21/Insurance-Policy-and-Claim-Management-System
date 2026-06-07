package com.monocept.project.repository;

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
}
