package com.monocept.project.repository;

import com.monocept.project.model.ClaimDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    Page<ClaimDocument> findByClaim_ClaimId(Long claimId, Pageable pageable);
}
