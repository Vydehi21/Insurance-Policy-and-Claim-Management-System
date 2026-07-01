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
    public PremiumPaymentResponseDTO recordPayment(PremiumPaymentRequestDTO paymentRequestDTO) {
        log.info("Processing premium payment request for policy ID: {}", paymentRequestDTO.getPolicyId());

        Policy policy = policyRepository.findById(paymentRequestDTO.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

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

        // Auto-assign premium cost from core entity settings
        BigDecimal exactPremiumDue = (policy.getPolicyPlan() != null) ? policy.getPolicyPlan().getPremiumAmount() : paymentRequestDTO.getAmount();

        PremiumPayment payment = new PremiumPayment();
        payment.setPolicy(policy);
        payment.setAmount(exactPremiumDue);
        payment.setPaymentMode(paymentRequestDTO.getPaymentMode());
        payment.setTransactionReference(paymentRequestDTO.getTransactionReference());
        payment.setPaymentStatus(paymentRequestDTO.getPaymentStatus());
        payment.setPaymentDate(LocalDateTime.now());

        PremiumPayment savedPayment = premiumPaymentRepository.save(payment);
        
        log.info("Payment record created. Payment id: {} Transaction reference: {}",
                savedPayment.getId(), savedPayment.getTransactionReference());

        if (paymentRequestDTO.getPaymentStatus() == PaymentStatus.SUCCESS) {
            if (policy.getTotalPremiumPaid() == null) {
                policy.setTotalPremiumPaid(BigDecimal.ZERO);
            }
            
            policy.setTotalPremiumPaid(policy.getTotalPremiumPaid().add(exactPremiumDue));

            // Set the lock out threshold 1 year into the future
            policy.setNextPremiumDueDate(LocalDate.now().plusYears(1));
            log.info("Policy {} next annual due date advanced to: {}", policy.getPolicyNumber(), policy.getNextPremiumDueDate());

            if (policy.getPolicyStatus() == PolicyStatus.PENDING_PAYMENT) {
                policy.setPolicyStatus(PolicyStatus.ACTIVE);
                log.info("Policy issued after payment. Policy number: {}", policy.getPolicyNumber());
            }

            policyRepository.save(policy);
        }

        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PremiumPaymentResponseDTO getPaymentById(Long paymentId) {
        log.info("Fetching payment details with ID: {}", paymentId);
        PremiumPayment payment = premiumPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
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
            Long policyId, int page, int size, String sortBy, String direction) {
        log.info("Fetching paginated payments for policy ID: {}", policyId);

        Pageable pageable = createPageable(page, size, sortBy, direction);
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

        Pageable pageable = createPageable(page, size, sortBy, direction);
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

        Pageable pageable = createPageable(page, size, sortBy, direction);
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

        Pageable pageable = createPageable(page, size, sortBy, direction);
        Page<PremiumPaymentResponseDTO> result = premiumPaymentRepository
                .findByTransactionReferenceContainingIgnoreCase(reference, pageable)
                .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<PremiumPaymentResponseDTO> getAllPayments(
            int page, int size, String sortBy, String direction) {
        log.info("Fetching all tracking payments logged system-wide across agents");

        Pageable pageable = createPageable(page, size, sortBy, direction);
        Page<PremiumPaymentResponseDTO> result = premiumPaymentRepository.findAll(pageable)
                .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(result, sortBy, direction);
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            log.warn("LOG-017 Invalid pagination request. Negative page index supplied: {}", page);
            throw new InvalidRequestException("Page number cannot be negative.");
        }
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
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
            
            // --- EXTRACT NESTED CUSTOMER FULL NAME SAFELY ---
            if (payment.getPolicy().getCustomer() != null && 
                payment.getPolicy().getCustomer().getUser() != null) {
                
                dto.setCustomerName(payment.getPolicy().getCustomer().getUser().getFullName());
            }
        }
        return dto;
    }


}