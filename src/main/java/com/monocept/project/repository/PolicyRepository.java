package com.monocept.project.repository;

import com.monocept.project.model.Policy;
import com.monocept.project.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyNumber(String policyNumber);
    boolean existsByPolicyNumber(String policyNumber);
    Page<Policy> findByPolicyStatus(PolicyStatus policyStatus, Pageable pageable);
    Page<Policy> findByCustomer_CustomerId(Long customerId, Pageable pageable);
    Page<Policy> findByCustomer_CustomerIdAndPolicyStatus(Long customerId, PolicyStatus policyStatus, Pageable pageable);
    Page<Policy> findByPolicyNumberContainingIgnoreCase(String policyNumber, Pageable pageable);
}
