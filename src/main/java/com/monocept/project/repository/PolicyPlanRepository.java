package com.monocept.project.repository;

import com.monocept.project.model.PolicyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PolicyPlanRepository extends JpaRepository<PolicyPlan, Long> {
    List<PolicyPlan> findByInsuranceProduct_ProductId(Long productId);
    List<PolicyPlan> findByActiveStatusTrue();
}
