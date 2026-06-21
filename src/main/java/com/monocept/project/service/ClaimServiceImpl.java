package com.monocept.project.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.ClaimDocumentDTO;
import com.monocept.project.dto.ClaimFinalDecisionRequestDTO;
import com.monocept.project.dto.ClaimRequestDTO;
import com.monocept.project.dto.ClaimResponseDTO;
import com.monocept.project.dto.ClaimReviewRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.enums.Role;
import com.monocept.project.exception.AuthorizationException;
import com.monocept.project.exception.InvalidStatusException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.Claim;
import com.monocept.project.model.ClaimDocument;
import com.monocept.project.model.ClaimStatusHistory;
import com.monocept.project.model.Customer;
import com.monocept.project.model.Policy;
import com.monocept.project.model.User;
import com.monocept.project.repository.ClaimDocumentRepository;
import com.monocept.project.repository.ClaimRepository;
import com.monocept.project.repository.ClaimStatusHistoryRepository;
import com.monocept.project.repository.CustomerRepository;
import com.monocept.project.repository.PolicyRepository;
import com.monocept.project.repository.UserRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ClaimDocumentRepository claimDocumentRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final ModelMapper modelMapper;

    private String generateClaimNumber() {
        return "CLM-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private void createHistory(
            Claim claim,
            ClaimStatus previousStatus,
            ClaimStatus newStatus,
            String remarks,
            User updatedBy) {

        ClaimStatusHistory history = new ClaimStatusHistory();

        history.setClaim(claim);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setRemarks(remarks);
        history.setUser(updatedBy);

        claimStatusHistoryRepository.save(history);
    }

    private ClaimResponseDTO convertToResponseDTO(Claim claim) {

        ClaimResponseDTO dto = modelMapper.map(
                claim,
                ClaimResponseDTO.class);

        dto.setPolicyNumber(
                claim.getPolicy().getPolicyNumber());

        dto.setCustomerName(
                claim.getPolicy()
                     .getCustomer()
                     .getUser()
                     .getFullName());

        return dto;
    }

    private List<ClaimDocument> buildDocuments(
            Claim claim,
            List<ClaimDocumentDTO> documentDTOs) {

        List<ClaimDocument> documents = new ArrayList<>();

        for (ClaimDocumentDTO dto : documentDTOs) {

            ClaimDocument document =
                    new ClaimDocument();

            document.setClaim(claim);
            document.setDocumentName(
                    dto.getDocumentName());

            document.setDocumentType(
                    dto.getDocumentType());

            document.setDocumentReference(
                    dto.getDocumentReference());

            documents.add(document);
        }

        return documents;
    }

    private User getUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: "
                                        + userId));
    }

    private Claim getClaim(Long claimId) {

        return claimRepository.findById(claimId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Claim not found with id: "
                                        + claimId));
    }

    private Policy getPolicy(Long policyId) {

        return policyRepository.findById(policyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Policy not found with id: "
                                        + policyId));
    }

    private Customer getCustomerByUserId(
            Long userId) {

        return customerRepository
                .findByUser_Id(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer profile not found"));
    }
    
//    @Override
//    @Transactional
//    public ClaimResponseDTO raiseClaim(
//            Long authenticatedUserId,
//            ClaimRequestDTO claimRequestDTO) {
//
//        Customer customer =
//                getCustomerByUserId(authenticatedUserId);
//
//        Policy policy =
//                getPolicy(claimRequestDTO.getPolicyId());
//
//        if (!policy.getCustomer()
//                .getId()
//                .equals(customer.getId())) {
//
//            throw new AuthorizationException(
//                    "You can only raise claims for your own policies");
//        }
//
//        if (policy.getPolicyStatus() != PolicyStatus.ACTIVE) {
//
//            throw new BusinessRuleException(
//                    "Claims can only be raised for active policies");
//        }
//
//        if (claimRequestDTO.getClaimAmount()
//                .compareTo(
//                        policy.getPolicyPlan()
//                              .getCoverageAmount()) > 0) {
//
//            throw new BusinessRuleException(
//                    "Claim amount exceeds policy coverage amount");
//        }
//
//        Claim claim = new Claim();
//
//        claim.setClaimNumber(generateClaimNumber());
//
//        claim.setPolicy(policy);
//
//        claim.setClaimAmount(
//                claimRequestDTO.getClaimAmount());
//
//        claim.setClaimReason(
//                claimRequestDTO.getClaimReason());
//
//        claim.setIncidentDate(
//                claimRequestDTO.getIncidentDate());
//
//        claim.setClaimStatus(
//                ClaimStatus.SUBMITTED);
//
//        Claim savedClaim =
//                claimRepository.save(claim);
//
//        List<ClaimDocument> documents =
//                buildDocuments(
//                        savedClaim,
//                        claimRequestDTO
//                                .getSupportingDocuments());
//
//        claimDocumentRepository.saveAll(documents);
//
//        savedClaim.setClaimDocuments(documents);
//
//        User customerUser =
//                getUser(authenticatedUserId);
//
//        createHistory(
//                savedClaim,
//                ClaimStatus.SUBMITTED,
//                ClaimStatus.SUBMITTED,
//                "Claim submitted",
//                customerUser);
//
//        return convertToResponseDTO(savedClaim);
//    }
    
    @Override
    @Transactional
    public ClaimResponseDTO raiseClaim(
            Long userId,
            ClaimRequestDTO dto) {

        log.info("Raising claim for user id: {}", userId);


        Policy policy = policyRepository
                .findById(dto.getPolicyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Policy not found"
                        ));


        if(!policy.getCustomer()
                .getUser()
                .getId()
                .equals(userId)) {

            throw new AuthorizationException(
                    "You cannot claim another customer's policy"
            );
        }


        Claim claim = new Claim();


        claim.setClaimNumber(
                "CLM-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0,8)
                        .toUpperCase()
        );


        claim.setPolicy(policy);

        claim.setClaimAmount(
                dto.getClaimAmount()
        );

        claim.setClaimReason(
                dto.getClaimReason()
        );

        claim.setIncidentDate(
                dto.getIncidentDate()
        );


        claim.setClaimStatus(
                ClaimStatus.SUBMITTED
        );


        List<ClaimDocument> documents =
                dto.getSupportingDocuments()
                        .stream()
                        .map(documentDTO -> {

                            ClaimDocument document =
                                    new ClaimDocument();

                            document.setClaim(claim);

                            document.setDocumentName(
                                    documentDTO.getDocumentName()
                            );

                            document.setDocumentType(
                                    documentDTO.getDocumentType()
                            );

                            document.setDocumentReference(
                                    documentDTO.getDocumentReference()
                            );

                            return document;

                        }).toList();


        claim.setClaimDocuments(
                documents
        );


        Claim savedClaim =
                claimRepository.save(claim);

        return modelMapper.map(
                savedClaim,
                ClaimResponseDTO.class
        );
    }
    
    @Override
    @Transactional
    public ClaimResponseDTO reviewClaim(
            Long claimId,
            Long agentUserId,
            ClaimReviewRequestDTO reviewDTO) {

        User agent = getUser(agentUserId);

        if (agent.getRole() != Role.AGENT) {
            throw new AuthorizationException(
                    "Only agents can review claims");
        }

        Claim claim = getClaim(claimId);

        if (claim.getClaimStatus() != ClaimStatus.SUBMITTED) {
            throw new InvalidStatusException(
                    "Only submitted claims can be reviewed");
        }

        if (reviewDTO.getRecommendedStatus() != ClaimStatus.RECOMMENDED_APPROVAL
                && reviewDTO.getRecommendedStatus() != ClaimStatus.RECOMMENDED_REJECTION) {

            throw new InvalidStatusException(
                    "Agent can only recommend approval or rejection");
        }

        ClaimStatus previousStatus =
                claim.getClaimStatus();

        claim.setClaimStatus(
                reviewDTO.getRecommendedStatus());

        claim.setAgentRemarks(
                reviewDTO.getRemarks());

        Claim updatedClaim =
                claimRepository.save(claim);

        createHistory(
                updatedClaim,
                previousStatus,
                reviewDTO.getRecommendedStatus(),
                reviewDTO.getRemarks(),
                agent);

        return convertToResponseDTO(updatedClaim);
    }
    
    @Override
    @Transactional
    public ClaimResponseDTO processFinalDecision(
            Long claimId,
            Long adminUserId,
            ClaimFinalDecisionRequestDTO decisionDTO) {

        User admin = getUser(adminUserId);

        if (admin.getRole() != Role.ADMIN) {
            throw new AuthorizationException(
                    "Only admins can make final claim decisions");
        }

        Claim claim = getClaim(claimId);

        if (claim.getClaimStatus() != ClaimStatus.RECOMMENDED_APPROVAL
                && claim.getClaimStatus() != ClaimStatus.RECOMMENDED_REJECTION) {

            throw new InvalidStatusException(
                    "Claim must be recommended before final decision");
        }

        if (decisionDTO.getFinalDecisionStatus() != ClaimStatus.APPROVED
                && decisionDTO.getFinalDecisionStatus() != ClaimStatus.REJECTED) {

            throw new InvalidStatusException(
                    "Final decision must be APPROVED or REJECTED");
        }

        if (claim.getClaimStatus() == ClaimStatus.RECOMMENDED_APPROVAL
                && decisionDTO.getFinalDecisionStatus() != ClaimStatus.APPROVED) {

            throw new InvalidStatusException(
                    "Recommended approval claims can only be approved");
        }

        if (claim.getClaimStatus() == ClaimStatus.RECOMMENDED_REJECTION
                && decisionDTO.getFinalDecisionStatus() != ClaimStatus.REJECTED) {

            throw new InvalidStatusException(
                    "Recommended rejection claims can only be rejected");
        }

        ClaimStatus previousStatus =
                claim.getClaimStatus();

        claim.setClaimStatus(
                decisionDTO.getFinalDecisionStatus());

        claim.setAdminRemarks(
                decisionDTO.getRemarks());

        Claim updatedClaim =
                claimRepository.save(claim);

        createHistory(
                updatedClaim,
                previousStatus,
                decisionDTO.getFinalDecisionStatus(),
                decisionDTO.getRemarks(),
                admin);

        return convertToResponseDTO(updatedClaim);
    }
    @Override
    @Transactional(readOnly = true)
    public ClaimResponseDTO getClaimById(Long claimId) {

        Claim claim = getClaim(claimId);

        return convertToResponseDTO(claim);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<ClaimResponseDTO> getAllClaims(
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.fromString(direction),
                        sortBy));

        Page<Claim> claimPage =
                claimRepository.findAll(pageable);

        Page<ClaimResponseDTO> dtoPage =
                claimPage.map(this::convertToResponseDTO);

        return PaginationUtil.createPaginatedResponse(
                dtoPage,
                sortBy,
                direction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<ClaimResponseDTO> getClaimsByCustomerId(
            Long customerId,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.fromString(direction),
                        sortBy));

        Page<Claim> claimPage =

                claimRepository.findByPolicy_Customer_Id(

                

                        customerId,
                        pageable);

        Page<ClaimResponseDTO> dtoPage =
                claimPage.map(this::convertToResponseDTO);

        return PaginationUtil.createPaginatedResponse(
                dtoPage,
                sortBy,
                direction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<ClaimResponseDTO> getClaimsByStatus(
            ClaimStatus status,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.fromString(direction),
                        sortBy));

        Page<Claim> claimPage =
                claimRepository.findByClaimStatus(
                        status,
                        pageable);

        Page<ClaimResponseDTO> dtoPage =
                claimPage.map(this::convertToResponseDTO);

        return PaginationUtil.createPaginatedResponse(
                dtoPage,
                sortBy,
                direction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<ClaimResponseDTO> getClaimsByCustomerAndStatus(
            Long customerId,
            ClaimStatus status,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.fromString(direction),
                        sortBy));

        Page<Claim> claimPage =
                claimRepository

                        .findByPolicy_Customer_IdAndClaimStatus(

                                customerId,
                                status,
                                pageable);

        Page<ClaimResponseDTO> dtoPage =
                claimPage.map(this::convertToResponseDTO);

        return PaginationUtil.createPaginatedResponse(
                dtoPage,
                sortBy,
                direction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<ClaimResponseDTO> searchClaimsByNumber(
            String claimNumber,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.fromString(direction),
                        sortBy));

        Page<Claim> claimPage =
                claimRepository
                        .findByClaimNumberContainingIgnoreCase(
                                claimNumber,
                                pageable);

        Page<ClaimResponseDTO> dtoPage =
                claimPage.map(this::convertToResponseDTO);

        return PaginationUtil.createPaginatedResponse(
                dtoPage,
                sortBy,
                direction);
    }
    
}