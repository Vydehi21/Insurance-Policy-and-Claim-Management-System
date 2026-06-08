package com.monocept.project.repository;

import com.monocept.project.model.Claim;
import com.monocept.project.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimNumber(String claimNumber);
    boolean existsByClaimNumber(String claimNumber);
    Page<Claim> findByClaimStatus(ClaimStatus claimStatus, Pageable pageable);
    Page<Claim> findByPolicyCustomerId(Long customerId, Pageable pageable);
    Page<Claim> findByPolicyCustomerIdAndClaimStatus(Long customerId, ClaimStatus claimStatus, Pageable pageable);
    Page<Claim> findByPolicyId(Long policyId, Pageable pageable);
    Page<Claim> findByClaimNumberContainingIgnoreCase(String claimNumber, Pageable pageable);
}
