package com.monocept.project.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PremiumPaymentRequestDTO;
import com.monocept.project.dto.PremiumPaymentResponseDTO;
import com.monocept.project.enums.PaymentStatus;
import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.enums.PremiumType;
import com.monocept.project.exception.AuthorizationException;
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.DuplicateResourceException;
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

        if (policy.getPolicyStatus() == PolicyStatus.CANCELLED) {
            log.warn("Business rule violation. Payment attempted on cancelled policy: {}", policy.getPolicyNumber());
            throw new BusinessRuleException("Cannot record payment for a cancelled policy");
        }

        if (policy.getPolicyStatus() == PolicyStatus.EXPIRED) {
            log.warn("Business rule violation. Payment attempted on expired policy: {}", policy.getPolicyNumber());
            throw new BusinessRuleException("Cannot record payment for an expired policy");
        }

        // NEW: premium must be paid before the policy's own start date arrives. Once
        // the start date has passed without payment, the policy is left to lapse
        // (see PolicyServiceImpl.expireOverduePolicies) rather than being activated late.
        if (policy.getPolicyStatus() == PolicyStatus.PENDING_PAYMENT
                && LocalDate.now().isAfter(policy.getStartDate())) {
            log.warn("Business rule violation. Payment attempted after start date for unpaid policy: {} (start date: {})",
                    policy.getPolicyNumber(), policy.getStartDate());
            throw new BusinessRuleException(
                    "The payment window for this policy has closed. Premium must be paid on or before the start date: "
                            + policy.getStartDate());
        }

        if (premiumPaymentRepository.existsByTransactionReference(paymentRequestDTO.getTransactionReference())) {
            log.warn("Duplicate payment transaction reference: {}", paymentRequestDTO.getTransactionReference());
            throw new DuplicateResourceException("Transaction reference already exists");
        }

        PremiumType premiumType = policy.getPolicyPlan().getPremiumType();
        BigDecimal requiredPremium = policy.getPolicyPlan().getPremiumAmount();
        BigDecimal paidAmount = paymentRequestDTO.getAmount();

        // --- ONE_TIME plans: exactly one payment is ever expected, covering the full policy term ---
        if (premiumType == PremiumType.ONE_TIME && policy.getPolicyStatus() == PolicyStatus.ACTIVE) {
            log.warn("Rejected payment attempt on already-paid ONE_TIME policy: {}", policy.getPolicyNumber());
            throw new BusinessRuleException(
                    "This policy has a one-time premium and has already been paid in full. No further payments are required or accepted.");
        }

        // --- Recurring plans (MONTHLY / QUARTERLY / ANNUAL): block payment before the next due date ---
        if (policy.getPolicyStatus() == PolicyStatus.ACTIVE
                && policy.getNextPremiumDueDate() != null
                && LocalDate.now().isBefore(policy.getNextPremiumDueDate())) {
            log.warn("Business rule violation. Payment blocked. Current premium cycle already satisfied for Policy: {}. Next due date: {}",
                    policy.getPolicyNumber(), policy.getNextPremiumDueDate());
            throw new BusinessRuleException(
                    "Premium for the current cycle is already fully paid. Next payment is not due until: "
                            + policy.getNextPremiumDueDate());
        }

        // --- Reject anything that isn't an exact match: no partial payments (PMTRUL-008), no overpayment (no refunds - OOS-011) ---
        int comparison = paidAmount.compareTo(requiredPremium);
        if (comparison < 0) {
            log.warn("Rejected underpayment on policy {}. Paid: {}, required: {}",
                    policy.getPolicyNumber(), paidAmount, requiredPremium);
            throw new BusinessRuleException(
                    "Payment amount is less than the required premium of " + requiredPremium
                            + ". Partial payments are not supported; please pay the exact amount due.");
        }
        if (comparison > 0) {
            log.warn("Rejected overpayment on policy {}. Paid: {}, required: {}",
                    policy.getPolicyNumber(), paidAmount, requiredPremium);
            throw new BusinessRuleException(
                    "Payment amount exceeds the required premium of " + requiredPremium
                            + ". Overpayment cannot be accepted since refunds are not supported. Please pay the exact amount due.");
        }

        PremiumPayment payment = new PremiumPayment();
        payment.setPolicy(policy);
        payment.setAmount(paidAmount);
        payment.setPaymentMode(paymentRequestDTO.getPaymentMode());
        payment.setTransactionReference(paymentRequestDTO.getTransactionReference());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        PremiumPayment savedPayment = premiumPaymentRepository.save(payment);

        log.info("Payment record created successfully. Payment id: {} Transaction reference: {}",
                savedPayment.getId(), savedPayment.getTransactionReference());

        if (policy.getTotalPremiumPaid() == null) {
            policy.setTotalPremiumPaid(BigDecimal.ZERO);
        }
        policy.setTotalPremiumPaid(policy.getTotalPremiumPaid().add(paidAmount));

        // Enforces PAYBR-007: the required premium, paid in full, activates the policy.
        if (policy.getPolicyStatus() == PolicyStatus.PENDING_PAYMENT) {
            policy.setPolicyStatus(PolicyStatus.ACTIVE);
            log.info("Policy activated after full premium payment. Policy number: {}", policy.getPolicyNumber());
        }

        // Schedule the next due date according to the plan's actual premium type.
        // ONE_TIME plans never expect another payment.
        switch (premiumType) {
            case MONTHLY -> policy.setNextPremiumDueDate(LocalDate.now().plusMonths(1));
            case QUARTERLY -> policy.setNextPremiumDueDate(LocalDate.now().plusMonths(3));
            case ANNUAL -> policy.setNextPremiumDueDate(LocalDate.now().plusYears(1));
            case ONE_TIME -> policy.setNextPremiumDueDate(null);
        }

        if (policy.getNextPremiumDueDate() != null) {
            log.info("Policy {} next premium due date set to: {}", policy.getPolicyNumber(), policy.getNextPremiumDueDate());
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