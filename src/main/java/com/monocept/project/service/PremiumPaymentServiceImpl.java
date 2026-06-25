package com.monocept.project.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
import com.monocept.project.service.PremiumPaymentService;
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
    public PremiumPaymentResponseDTO recordPayment(
            PremiumPaymentRequestDTO paymentRequestDTO) {

        Policy policy = policyRepository.findById(
                paymentRequestDTO.getPolicyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Policy not found"));

        if (premiumPaymentRepository.existsByTransactionReference(
                paymentRequestDTO.getTransactionReference())) {
        	
        	log.warn(
                    "Duplicate payment transaction reference: {}",
                    paymentRequestDTO.getTransactionReference()
            );

            throw new DuplicateResourceException(
                    "Transaction reference already exists");
        }

        if (policy.getPolicyStatus() == PolicyStatus.CANCELLED) {
        	
        	 log.warn(
        	            "Business rule violation. Payment attempted on cancelled policy: {}",
        	            policy.getPolicyNumber()
        	    );
        	 
            throw new BusinessRuleException(
                    "Cannot record payment for cancelled policy");
        }

        PremiumPayment payment = new PremiumPayment();

        payment.setPolicy(policy);
        payment.setAmount(paymentRequestDTO.getAmount());
        payment.setPaymentMode(paymentRequestDTO.getPaymentMode());
        payment.setTransactionReference(
                paymentRequestDTO.getTransactionReference());
        payment.setPaymentStatus(
                paymentRequestDTO.getPaymentStatus());

        PremiumPayment savedPayment =
                premiumPaymentRepository.save(payment);
        
        log.info(
                "Payment record created. Payment id: {} Transaction reference: {}",
                savedPayment.getId(),
                savedPayment.getTransactionReference()
        );

        if (paymentRequestDTO.getPaymentStatus()
                == PaymentStatus.SUCCESS) {

            policy.setTotalPremiumPaid(
                    policy.getTotalPremiumPaid()
                            .add(paymentRequestDTO.getAmount()));

            // Activate only when policy is awaiting first payment
            if (policy.getPolicyStatus()
                    == PolicyStatus.PENDING_PAYMENT) {

                policy.setPolicyStatus(PolicyStatus.ACTIVE);
                
                log.info(
                        "Policy issued after payment. Policy number: {}",
                        policy.getPolicyNumber()
                );
            }

            policyRepository.save(policy);
        }

        return mapToResponse(savedPayment);
    }

    @Override
    public PremiumPaymentResponseDTO getPaymentById(Long paymentId) {

        PremiumPayment payment =
                premiumPaymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"));

        return mapToResponse(payment);
    }

    @Override
    public PaginatedResponseDTO<PremiumPaymentResponseDTO>
            getPaymentsByPolicyId(
                    Long policyId,
                    int page,
                    int size,
                    String sortBy,
                    String direction) {

        Pageable pageable =
                createPageable(page, size, sortBy, direction);

        Page<PremiumPaymentResponseDTO> result =
                premiumPaymentRepository
                        .findByPolicy_Id(policyId, pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<PremiumPaymentResponseDTO>
            getPaymentsByStatus(
                    PaymentStatus status,
                    int page,
                    int size,
                    String sortBy,
                    String direction) {

        Pageable pageable =
                createPageable(page, size, sortBy, direction);

        Page<PremiumPaymentResponseDTO> result =
                premiumPaymentRepository
                        .findByPaymentStatus(status, pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<PremiumPaymentResponseDTO>
            getPaymentsByPolicyAndStatus(
                    Long policyId,
                    PaymentStatus status,
                    int page,
                    int size,
                    String sortBy,
                    String direction) {

        Pageable pageable =
                createPageable(page, size, sortBy, direction);

        Page<PremiumPaymentResponseDTO> result =
                premiumPaymentRepository
                        .findByPolicyIdAndPaymentStatus(
                                policyId,
                                status,
                                pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    @Override
    public PaginatedResponseDTO<PremiumPaymentResponseDTO>
            searchPaymentsByReference(
                    String reference,
                    int page,
                    int size,
                    String sortBy,
                    String direction) {

        Pageable pageable =
                createPageable(page, size, sortBy, direction);

        Page<PremiumPaymentResponseDTO> result =
                premiumPaymentRepository
                        .findByTransactionReferenceContainingIgnoreCase(
                                reference,
                                pageable)
                        .map(this::mapToResponse);

        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction);
    }

    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String direction) {


        if(page < 0) {

            log.warn(
                    "LOG-017 Invalid pagination request. Negative page: {}",
                    page
            );

            throw new InvalidRequestException(
                    "Page number cannot be negative");
        }


        if(size <=0 || size >100) {

            log.warn(
                    "LOG-017 Invalid pagination request. Invalid size: {}",
                    size
            );

            throw new InvalidRequestException(
                    "Page size must be between 1 and 100");
        }


        Sort sort =
                direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();


        return PageRequest.of(
                page,
                size,
                sort);
    }

    private PremiumPaymentResponseDTO mapToResponse(
            PremiumPayment payment) {

        PremiumPaymentResponseDTO dto =
                new PremiumPaymentResponseDTO();

        dto.setPaymentId(payment.getId());
        dto.setPolicyNumber(
                payment.getPolicy().getPolicyNumber());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMode(payment.getPaymentMode());
        dto.setTransactionReference(
                payment.getTransactionReference());
        dto.setPaymentStatus(payment.getPaymentStatus());

        return dto;
    }
    
    public List<PremiumPaymentResponseDTO> getPaymentsByCustomer(Long userId){

        Customer customer =
            customerRepository.findByUser_Id(userId)
            .orElseThrow(
                () -> new RuntimeException("Customer not found")
            );


        return premiumPaymentRepository
                .findByPolicy_Customer_Id(
                        customer.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();

    }
    
    @Override
    public PaginatedResponseDTO<PremiumPaymentResponseDTO>
    getAllPayments(
            int page,
            int size,
            String sortBy,
            String direction
    ){

        Pageable pageable =
                createPageable(
                        page,
                        size,
                        sortBy,
                        direction
                );


        Page<PremiumPaymentResponseDTO> result =
                premiumPaymentRepository
                .findAll(pageable)
                .map(this::mapToResponse);


        return PaginationUtil.createPaginatedResponse(
                result,
                sortBy,
                direction
        );

    }
}