package com.monocept.project.repository;

import com.monocept.project.model.PremiumPayment;
import com.monocept.project.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, Long> {
    Optional<PremiumPayment> findByTransactionReference(String transactionReference);
    Page<PremiumPayment> findByPolicy_PolicyId(Long policyId, Pageable pageable);
    Page<PremiumPayment> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);
    Page<PremiumPayment> findByTransactionReferenceContainingIgnoreCase(String transactionReference, Pageable pageable);
}
