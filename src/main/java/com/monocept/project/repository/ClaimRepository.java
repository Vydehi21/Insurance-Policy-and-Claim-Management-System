package com.monocept.project.repository;

import com.monocept.project.model.Claim;
import com.monocept.project.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimNumber(String claimNumber);
    List<Claim> findByPolicy_PolicyId(Long policyId);
    List<Claim> findByClaimStatus(ClaimStatus claimStatus);
    List<Claim> findByPolicy_Customer_CustomerId(Long customerId);
}
