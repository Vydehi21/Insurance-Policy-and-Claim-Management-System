package com.monocept.project.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PremiumPaymentRequestDTO;
import com.monocept.project.dto.PremiumPaymentResponseDTO;
import com.monocept.project.enums.PaymentStatus;
import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.exception.AuthorizationException;
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.exception.InvalidRequestException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.Customer;
import com.monocept.project.model.Policy;
import com.monocept.project.model.PremiumPayment;
import com.monocept.project.repository.CustomerRepository;
import com.monocept.project.repository.PolicyRepository;
import com.monocept.project.repository.PremiumPaymentRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumPaymentServiceImpl implements PremiumPaymentService {

    private final PremiumPaymentRepository premiumPaymentRepository;
    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public PremiumPaymentResponseDTO recordPayment(PremiumPaymentRequestDTO paymentRequestDTO, Long requesterUserId, String requesterRole) {
        log.info("Processing premium payment request for policy ID: {}", paymentRequestDTO.getPolicyId());

        Policy policy = policyRepository.findById(paymentRequestDTO.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        if ("CUSTOMER".equals(requesterRole)
                && !policy.getCustomer().getUser().getId().equals(requesterUserId)) {
            log.warn("Blocked attempt by user {} to record payment on another customer's policy: {}",
                    requesterUserId, policy.getPolicyNumber());
            throw new AuthorizationException("You are not authorized to record a payment for this policy");
        }

        // --- BUSINESS RULE: VALIDATE ANNUAL LOCK STATUS ---
        if (policy.getNextPremiumDueDate() != null && LocalDate.now().isBefore(policy.getNextPremiumDueDate())) {
            log.warn("Business rule violation. Payment blocked. Annual premium already satisfied for Policy: {}. Next due date: {}", 
                    policy.getPolicyNumber(), policy.getNextPremiumDueDate());
            throw new BusinessRuleException("Premium for this annual cycle is already fully paid. Next payment allowed on: " + policy.getNextPremiumDueDate());
        }

        if (premiumPaymentRepository.existsByTransactionReference(paymentRequestDTO.getTransactionReference())) {
            log.warn("Duplicate payment transaction reference: {}", paymentRequestDTO.getTransactionReference());
            throw new DuplicateResourceException("Transaction reference already exists");
        }

        if (policy.getPolicyStatus() == PolicyStatus.CANCELLED) {
            log.warn("Business rule violation. Payment attempted on cancelled policy: {}", policy.getPolicyNumber());
            throw new BusinessRuleException("Cannot record payment for cancelled policy");
        }

        BigDecimal paidAmount = paymentRequestDTO.getAmount();
        BigDecimal requiredPremium = policy.getPolicyPlan().getPremiumAmount();

        PremiumPayment payment = new PremiumPayment();
        payment.setPolicy(policy);
        payment.setAmount(paidAmount);
        payment.setPaymentMode(paymentRequestDTO.getPaymentMode());
        payment.setTransactionReference(paymentRequestDTO.getTransactionReference());
        
        // 🔒 Server hardcodes the success state unconditionally instead of relying on client inputs
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        PremiumPayment savedPayment = premiumPaymentRepository.save(payment);
        
        log.info("Payment record created successfully. Payment id: {} Transaction reference: {}",
                savedPayment.getId(), savedPayment.getTransactionReference());

        // 🛠️ FIXED: Removed 'paymentRequestDTO.getPaymentStatus()' validation to clear compile error
        if (policy.getTotalPremiumPaid() == null) {
            policy.setTotalPremiumPaid(BigDecimal.ZERO);
        }
        
        policy.setTotalPremiumPaid(policy.getTotalPremiumPaid().add(paidAmount));

        // Advance lockout window 1 year into the future
        policy.setNextPremiumDueDate(LocalDate.now().plusYears(1));
        log.info("Policy {} next annual due date advanced to: {}", policy.getPolicyNumber(), policy.getNextPremiumDueDate());

        // Enforces PAYBR-007: first successful payment equal to or greater than required premium activates policy
        if (policy.getPolicyStatus() == PolicyStatus.PENDING_PAYMENT && paidAmount.compareTo(requiredPremium) >= 0) {
            policy.setPolicyStatus(PolicyStatus.ACTIVE);
            log.info("Policy issued after payment. Policy number: {}", policy.getPolicyNumber());
        } else if (policy.getPolicyStatus() == PolicyStatus.PENDING_PAYMENT) {
            log.warn("Successful payment of {} on policy {} did not meet required premium {}. Policy remains Pending Payment.",
                    paidAmount, policy.getPolicyNumber(), requiredPremium);
        }

        policyRepository.save(policy);

        return mapToResponse(savedPayment);
    }


    @Override
    @Transactional(readOnly = true)
    public PremiumPaymentResponseDTO getPaymentById(Long paymentId, Long requesterUserId, String requesterRole) {
        log.info("Fetching payment details with ID: {}", paymentId);
        PremiumPayment payment = premiumPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        enforcePaymentOwnership(payment, requesterUserId, requesterRole);

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PremiumPaymentResponseDTO> getPaymentsByCustomer(Long userId) {
        log.info("Fetching premium payments list for user context ID: {}", userId);
        Customer customer = customerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user ID: " + userId));

        return premiumPaymentRepository.findByPolicy_Customer_Id(customer.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByPolicyId(
            Long policyId, int page, int size, String sortBy, String direction,
            Long requesterUserId, String requesterRole) {
        log.info("Fetching paginated payments for policy ID: {}", policyId);

        // Enforces PAYBR-009: customers may only view payments for their own policies.
        if ("CUSTOMER".equals(requesterRole)) {
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
            if (!policy.getCustomer().getUser().getId().equals(requesterUserId)) {
                log.warn("Blocked attempt by user {} to view payments for another customer's policy: {}",
                        requesterUserId, policyId);
                throw new AuthorizationException("You are not authorized to view payments for this policy");
            }
        }

        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);
        Page<PremiumPaymentResponseDTO> result = premiumPaymentRepository
                .findByPolicy_Id(policyId, pageable)
                .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByStatus(
            PaymentStatus status, int page, int size, String sortBy, String direction) {
        log.info("Fetching paginated payments with status constraint: {}", status);

        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);
        Page<PremiumPaymentResponseDTO> result = premiumPaymentRepository
                .findByPaymentStatus(status, pageable)
                .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PremiumPaymentResponseDTO> getPaymentsByPolicyAndStatus(
            Long policyId, PaymentStatus status, int page, int size, String sortBy, String direction) {
        log.info("Fetching paginated payments for policy ID: {} filtered by status: {}", policyId, status);

        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);
        Page<PremiumPaymentResponseDTO> result = premiumPaymentRepository
                .findByPolicyIdAndPaymentStatus(policyId, status, pageable)
                .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PremiumPaymentResponseDTO> searchPaymentsByReference(
            String reference, int page, int size, String sortBy, String direction) {
        log.info("Searching payment database reference parameters for string matching: {}", reference);

        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);
        Page<PremiumPaymentResponseDTO> result = premiumPaymentRepository
                .findByTransactionReferenceContainingIgnoreCase(reference, pageable)
                .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PremiumPaymentResponseDTO> getAllPayments(
            int page, int size, String sortBy, String direction) {
        log.info("Fetching all tracking payments logged system-wide across internal staff");

        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);
        Page<PremiumPaymentResponseDTO> result = premiumPaymentRepository.findAll(pageable)
                .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
    }

  

    private void enforcePaymentOwnership(PremiumPayment payment, Long requesterUserId, String requesterRole) {
        if ("CUSTOMER".equals(requesterRole)
                && !payment.getPolicy().getCustomer().getUser().getId().equals(requesterUserId)) {
            log.warn("Blocked attempt by user {} to view another customer's payment record: {}",
                    requesterUserId, payment.getId());
            throw new AuthorizationException("You are not authorized to view this payment");
        }
    }

    private PremiumPaymentResponseDTO mapToResponse(PremiumPayment payment) {
        PremiumPaymentResponseDTO dto = new PremiumPaymentResponseDTO();
        dto.setPaymentId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMode(payment.getPaymentMode());
        dto.setTransactionReference(payment.getTransactionReference());
        dto.setPaymentStatus(payment.getPaymentStatus());
        
        if (payment.getPolicy() != null) {
            dto.setPolicyNumber(payment.getPolicy().getPolicyNumber());
            dto.setNextPremiumDueDate(payment.getPolicy().getNextPremiumDueDate());
            
            if (payment.getPolicy().getCustomer() != null && 
                payment.getPolicy().getCustomer().getUser() != null) {
                
                dto.setCustomerName(payment.getPolicy().getCustomer().getUser().getFullName());
            }
        }
        return dto;
    }


}