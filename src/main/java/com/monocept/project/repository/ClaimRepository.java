package com.monocept.project.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.model.Claim;

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
    
    boolean existsByPolicyIdAndClaimStatusIn(
            Long policyId,
            List<ClaimStatus> statuses);
    List<Claim> findByPolicy_Customer_Id(
            Long customerId
    );
    @Query("""
    		SELECT COALESCE(SUM(c.claimAmount),0)
    		FROM Claim c
    		WHERE c.policy.id = :policyId
    		AND c.claimStatus = 'APPROVED'
    		""")
    		BigDecimal getApprovedClaimAmount(
    		        @Param("policyId") Long policyId
    		);
    
    List<Claim> findByPolicy_Id(Long policyId);
    Page<Claim> findByClaimStatusIn(
            List<ClaimStatus> statuses,
            Pageable pageable
    );
    Optional<Claim> findByIdAndPolicy_Customer_User_Id(Long claimId, Long userId);
}
