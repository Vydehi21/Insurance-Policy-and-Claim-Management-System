package com.monocept.project.repository;

    import com.monocept.project.model.Policy;
    import com.monocept.project.enums.PolicyStatus;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;
    import java.util.Optional;

    @Repository
    public interface PolicyRepository extends JpaRepository<Policy, Long> {
        Optional<Policy> findByPolicyNumber(String policyNumber);
        boolean existsByPolicyNumber(String policyNumber);
        Page<Policy> findByPolicyStatus(PolicyStatus policyStatus, Pageable pageable);
        
        // FIXED: Changed CustomerId to Id
        Page<Policy> findByCustomer_Id(Long customerId, Pageable pageable);
        
        // FIXED: Changed CustomerId to Id
        Page<Policy> findByCustomer_IdAndPolicyStatus(Long customerId, PolicyStatus policyStatus, Pageable pageable);
        

        Page<Policy> findByPolicyNumberContainingIgnoreCase(String policyNumber, Pageable pageable);
        Optional<Policy> findTopByOrderByIdDesc();
        
        // SAFE FIX: Uses explicit JPQL to safely bind customer and plan relationship properties
        @Query("SELECT p FROM Policy p WHERE p.customer.id = :customerId AND p.policyPlan.id = :planId ORDER BY p.createdDate DESC LIMIT 1")
        Optional<Policy> findLatestPolicyByCustomerAndPlan(
                @Param("customerId") Long customerId,
                @Param("planId") Long planId);
        
        java.util.List<Policy> findByPolicyStatusAndEndDateBefore(PolicyStatus policyStatus, java.time.LocalDate date);

        // NEW: supports the sweep that lapses policies whose start date passed
        // without ever being paid (see PolicyServiceImpl.expireOverduePolicies)
        java.util.List<Policy> findByPolicyStatusAndStartDateBefore(PolicyStatus policyStatus, java.time.LocalDate date);
    }