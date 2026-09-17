package com.monocept.project.repository;

import com.monocept.project.model.ClaimStatusHistory;
import com.monocept.project.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {

    Page<ClaimStatusHistory> findByClaim_Id(Long claimId, Pageable pageable);
    
    // FIXED: Changed User_UserId to User_Id to target User primary key name
    Page<ClaimStatusHistory> findByUser_Id(
            Long userId,
            Pageable pageable);
            
    Page<ClaimStatusHistory> findByNewStatus(ClaimStatus status, Pageable pageable);
    
    // FIXED: Changed User_UserId to User_Id
    Page<ClaimStatusHistory> findByClaim_IdAndUser_IdAndNewStatus(
            Long claimId,
            Long userId,
            ClaimStatus status,
            Pageable pageable);
}
