package com.monocept.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monocept.project.enums.PaymentStatus;
import com.monocept.project.model.PremiumPayment;

@Repository
public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, Long> {
    Optional<PremiumPayment> findByTransactionReference(String transactionReference);
    boolean existsByTransactionReference(String transactionReference);
    Page<PremiumPayment> findByPolicy_Id(Long id, Pageable pageable);
    Page<PremiumPayment> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);
    Page<PremiumPayment> findByPolicyIdAndPaymentStatus(Long id, PaymentStatus paymentStatus, Pageable pageable);
    Page<PremiumPayment> findByTransactionReferenceContainingIgnoreCase(String transactionReference, Pageable pageable);
    
    List<PremiumPayment> findByPolicy_Customer_Id(Long customerId);
}
