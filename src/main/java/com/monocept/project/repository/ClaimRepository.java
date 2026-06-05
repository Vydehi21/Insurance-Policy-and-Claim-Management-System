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
    Page<Claim> findByPolicy_PolicyId(Long policyId, Pageable pageable);
    Page<Claim> findByClaimStatus(ClaimStatus claimStatus, Pageable pageable);
    Page<Claim> findByPolicy_Customer_CustomerId(Long customerId, Pageable pageable);
    Page<Claim> findByClaimNumberContainingIgnoreCase(String claimNumber, Pageable pageable);
}
