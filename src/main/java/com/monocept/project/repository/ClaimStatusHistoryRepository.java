package com.monocept.project.repository;

import com.monocept.project.model.ClaimStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {
    Page<ClaimStatusHistory> findByClaim_ClaimId(Long claimId, Pageable pageable);
}
