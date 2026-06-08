package com.monocept.project.repository;

import com.monocept.project.model.ClaimStatusHistory;
import com.monocept.project.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {

    Page<ClaimStatusHistory> findByClaimId(Long claimId, Pageable pageable);
    Page<ClaimStatusHistory> findByUserId(
            Long userId,
            Pageable pageable);
    Page<ClaimStatusHistory> findByNewStatus(ClaimStatus status, Pageable pageable);
    Page<ClaimStatusHistory> findByClaimIdAndUserIdAndNewStatus(
            Long claimId,
            Long userId,
            ClaimStatus status,
            Pageable pageable);
}
