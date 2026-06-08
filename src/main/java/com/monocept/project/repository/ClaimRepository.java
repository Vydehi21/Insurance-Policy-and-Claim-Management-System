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
    
    // FIXED: Changed CustomerId to Id to match Customer primary key name
    Page<Claim> findByPolicy_Customer_Id(Long customerId, Pageable pageable);
    
    // FIXED: Changed CustomerId to Id
    Page<Claim> findByPolicy_Customer_IdAndClaimStatus(Long customerId, ClaimStatus claimStatus, Pageable pageable);
    
    Page<Claim> findByPolicy_Id(Long policyId, Pageable pageable);
    
    // FIXED: Corrected the full method name syntax here
    Page<Claim> findByClaimNumberContainingIgnoreCase(String claimNumber, Pageable pageable);
}
