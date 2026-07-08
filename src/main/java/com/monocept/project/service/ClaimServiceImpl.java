package com.monocept.project.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
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
import com.monocept.project.dto.ClaimStatusHistoryResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ClaimStatus;
import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.enums.Role;
import com.monocept.project.exception.AuthorizationException;

import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.exception.InvalidRequestException;

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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional

public class ClaimServiceImpl implements ClaimService {

	private final ClaimRepository claimRepository;
	private final PolicyRepository policyRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final ClaimDocumentRepository claimDocumentRepository;
	private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
	private final ModelMapper modelMapper;

	@Value("${cloudinary.cloud-name}")
	private String cloudinaryCloudName;

	private static final String CLOUDINARY_HOST = "res.cloudinary.com";

	private String generateClaimNumber() {
		return "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private void createHistory(Claim claim, ClaimStatus previousStatus, ClaimStatus newStatus, String remarks,
			User updatedBy) {

		ClaimStatusHistory history = new ClaimStatusHistory();

		history.setClaim(claim);
		history.setPreviousStatus(previousStatus);
		history.setNewStatus(newStatus);
		history.setRemarks(remarks);
		history.setUser(updatedBy);

		claimStatusHistoryRepository.save(history);
	}

	private List<ClaimDocument> buildDocuments(Claim claim, List<ClaimDocumentDTO> documentDTOs) {

		List<ClaimDocument> documents = new ArrayList<>();

		if (documentDTOs == null) {
			return documents;
		}

		for (ClaimDocumentDTO dto : documentDTOs) {

			ClaimDocument document = new ClaimDocument();

			document.setClaim(claim);

			document.setDocumentName(dto.getDocumentName());

			document.setDocumentType(dto.getDocumentType());

			document.setDocumentReference(dto.getDocumentReference());

			documents.add(document);

		}

		return documents;
	}

	/**
	 * Ensures every supporting document attached to a claim is a real,
	 * reachable file hosted on this project's Cloudinary account, not just
	 * an arbitrary string typed into the request body.
	 */
	private void validateSupportingDocuments(List<ClaimDocumentDTO> documentDTOs) {

		if (documentDTOs == null || documentDTOs.isEmpty()) {
			throw new InvalidRequestException(
					"At least one supporting document must be submitted to raise a claim");
		}

		for (ClaimDocumentDTO documentDTO : documentDTOs) {
			validateDocumentReference(documentDTO.getDocumentReference());
		}
	}

	private void validateDocumentReference(String documentReference) {

		URI uri;

		try {
			uri = URI.create(documentReference);
		} catch (Exception e) {
			throw new InvalidRequestException("Supporting document reference is not a valid URL");
		}

		// STEP 1: Domain allowlist check. Blocks made-up links, and also
		// stops the server from being tricked into calling an arbitrary
		// attacker-controlled host in step 2 (SSRF guard).
		boolean isTrustedCloudinaryUrl = "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
				&& uri.getHost().equalsIgnoreCase(CLOUDINARY_HOST) && uri.getPath() != null
				&& uri.getPath().startsWith("/" + cloudinaryCloudName + "/");

		if (!isTrustedCloudinaryUrl) {
			log.warn("Rejected claim document with untrusted or malformed reference: {}", documentReference);
			throw new InvalidRequestException(
					"Supporting document must be a valid file uploaded via /api/files/upload");
		}

		// STEP 2: Live reachability check. Confirms the file actually exists
		// at Cloudinary instead of just looking like a plausible URL.
		try {
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

			HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(5))
					.method("HEAD", HttpRequest.BodyPublishers.noBody()).build();

			HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

			if (response.statusCode() != 200) {
				log.warn("Document verification failed. URL {} returned status {}", documentReference,
						response.statusCode());
				throw new InvalidRequestException(
						"Supporting document could not be verified (file not found at the provided URL). Please re-upload and try again.");
			}

		} catch (InvalidRequestException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error while verifying supporting document reference {}", documentReference, e);
			throw new InvalidRequestException(
					"Unable to verify supporting document right now. Please check the file and try again.");
		}
	}

	private User getUser(Long userId) {

		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
	}

	private Claim getClaim(Long claimId) {

		return claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));
	}

	private Policy getPolicy(Long policyId) {

		return policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
	}

	private Customer getCustomerByUserId(Long userId) {

		return customerRepository.findByUser_Id(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
	}

	@Override
	@Transactional
	public ClaimResponseDTO getClaimDetailsForReview(Long claimId, Long staffUserId) {
		log.info("Internal staff user {} is opening claim ID {} for inspection", staffUserId, claimId);

		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim record not found"));

		User currentStaff = getUser(staffUserId);

		// CONCURRENCY LOCK GUARD: If already under review, verify if the active
		// staff member holds the lock
		if (claim.getClaimStatus() == ClaimStatus.UNDER_REVIEW && claim.getReviewedBy() != null
				&& !claim.getReviewedBy().getId().equals(staffUserId)) {

			log.warn("Collision prevented: Internal staff {} blocked from accessing claim {} locked by internal staff {}", staffUserId,
					claimId, claim.getReviewedBy().getFullName());
			throw new BusinessRuleException(
					"Access Denied. This claim file is currently locked and being processed by: "
							+ claim.getReviewedBy().getFullName());
		}

		//  LOCK ACQUISITION: If the claim is brand new, lock it to this staff member
		// instantly upon opening
		if (claim.getClaimStatus() == ClaimStatus.SUBMITTED) {
			ClaimStatus previousStatus = claim.getClaimStatus();
			claim.setClaimStatus(ClaimStatus.UNDER_REVIEW);
			claim.setReviewedBy(currentStaff);
			claim = claimRepository.save(claim);

			createHistory(claim, previousStatus, ClaimStatus.UNDER_REVIEW, "Claim locked for inspection by internal staff.",
					currentStaff);
			log.info("Claim {} successfully locked under active review by internal staff {}", claim.getClaimNumber(),
					staffUserId);
		}

		return convertToResponseDTO(claim);
	}

	@Override
	@Transactional
	public ClaimResponseDTO raiseClaim(Long authenticatedUserId, ClaimRequestDTO dto) {

		log.info("Customer {} attempting to raise claim for policy {}", authenticatedUserId, dto.getPolicyId());

		Policy policy = getPolicy(dto.getPolicyId());
		
		if (!policy.getCustomer().getUser().getId().equals(authenticatedUserId)) {
			log.warn("Blocked attempt by user {} to raise claim on another customer's policy: {}", authenticatedUserId,
					policy.getPolicyNumber());
			throw new AuthorizationException("You cannot claim another customer's policy");
		}

		// Checked first and explicitly: a policy must be ACTIVE (i.e. required
		// premium already paid in full) before any claim can be raised against it.
		// This blocks PENDING_PAYMENT, EXPIRED, and CANCELLED policies alike.
		log.info("Policy {} current status at claim submission time: {}", policy.getPolicyNumber(),
				policy.getPolicyStatus());

		if (policy.getPolicyStatus() != PolicyStatus.ACTIVE) {
			log.warn("Business rule violation. Claim attempted on non-active policy: {} (status: {})",
					policy.getPolicyNumber(), policy.getPolicyStatus());
			throw new BusinessRuleException(
					"Claim can only be raised for active policies. This policy is currently: "
							+ policy.getPolicyStatus());
		}

		if (policy.getEndDate().isBefore(LocalDate.now())) {
			log.warn("Business rule violation. Claim attempted on expired policy: {}", policy.getPolicyNumber());
			throw new BusinessRuleException("Policy has expired");
		}

		// NEW: policyStatus == ACTIVE only reflects that payment has been made — it says
		// nothing about whether the coverage window has actually opened yet. Block claims
		// raised before the policy's own start date.
		if (policy.getStartDate().isAfter(LocalDate.now())) {
			log.warn("Business rule violation. Claim attempted before policy coverage start date: {} (starts: {})",
					policy.getPolicyNumber(), policy.getStartDate());
			throw new BusinessRuleException(
					"This policy's coverage has not started yet. Coverage begins on: " + policy.getStartDate());
		}

		if (dto.getIncidentDate().isAfter(LocalDate.now())) {
			log.warn("Business rule violation. Future incident date submitted: {}", dto.getIncidentDate());
			throw new BusinessRuleException("Incident date cannot be in future");
		}

		// NEW: the incident itself must fall within the coverage window — otherwise a
		// customer could claim for something that happened before the policy even existed.
		if (dto.getIncidentDate().isBefore(policy.getStartDate())) {
			log.warn("Business rule violation. Incident date {} predates policy coverage start {} for policy: {}",
					dto.getIncidentDate(), policy.getStartDate(), policy.getPolicyNumber());
			throw new BusinessRuleException(
					"Incident date cannot be before the policy's coverage start date: " + policy.getStartDate());
		}

		BigDecimal approvedClaims = claimRepository.getApprovedClaimAmount(policy.getId());

		// --- SAFE CONVERSION FOR FIRST-TIME CLAIMS ---
		if (approvedClaims == null) {
			approvedClaims = BigDecimal.ZERO;
		}

		BigDecimal totalClaimAmount = approvedClaims.add(dto.getClaimAmount());

		if (totalClaimAmount.compareTo(policy.getPolicyPlan().getCoverageAmount()) > 0) {
			log.warn("Business rule violation. Claim amount {} (cumulative {}) exceeds coverage {} for policy {}",
					dto.getClaimAmount(), totalClaimAmount, policy.getPolicyPlan().getCoverageAmount(),
					policy.getPolicyNumber());
			throw new BusinessRuleException("Claim exceeds remaining coverage amount");
		}

		boolean exists = claimRepository.existsByPolicyIdAndClaimStatusIn(policy.getId(),
				List.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW));

		if (exists) {
			log.warn("Business rule violation. Duplicate open claim attempted for policy: {}",
					policy.getPolicyNumber());
			throw new BusinessRuleException("A claim already exists for this policy");
		}

		validateSupportingDocuments(dto.getSupportingDocuments());

		Claim claim = new Claim();
		claim.setClaimNumber(generateClaimNumber());
		claim.setPolicy(policy);
		claim.setClaimAmount(dto.getClaimAmount());
		claim.setClaimReason(dto.getClaimReason());
		claim.setIncidentDate(dto.getIncidentDate());
		claim.setClaimStatus(ClaimStatus.SUBMITTED);

		Claim savedClaim = claimRepository.save(claim);

		List<ClaimDocument> documents = buildDocuments(savedClaim, dto.getSupportingDocuments());

		claimDocumentRepository.saveAll(documents);
		savedClaim.setClaimDocuments(documents);

		User customer = getUser(authenticatedUserId);

		createHistory(savedClaim, ClaimStatus.SUBMITTED, ClaimStatus.SUBMITTED, "Claim submitted", customer);

		log.info("Claim {} submitted successfully", savedClaim.getClaimNumber());

		return convertToResponseDTO(savedClaim);
	}

	@Override
	@Transactional
	public ClaimResponseDTO reviewClaim(Long claimId, Long staffUserId, ClaimReviewRequestDTO dto) {
		log.info("Internal staff user {} is attempting to process a review update for claim ID {}", staffUserId, claimId);

		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim file reference not found"));

		User staffMember = getUser(staffUserId);

		// --- PHASE 1: CONCURRENCY LOCK ENGINE ---
		// If the claim is brand new, automatically assign it to this staff member and set it
		// to UNDER_REVIEW
		if (claim.getClaimStatus() == ClaimStatus.SUBMITTED) {
			ClaimStatus previousStatus = claim.getClaimStatus();
			claim.setClaimStatus(ClaimStatus.UNDER_REVIEW);
			claim.setReviewedBy(staffMember);
			claim = claimRepository.save(claim);

			createHistory(claim, previousStatus, ClaimStatus.UNDER_REVIEW,
					"Claim assigned and locked for internal staff inspection.", staffMember);
			log.info("Claim {} successfully locked under active review by internal staff {}", claim.getClaimNumber(), staffUserId);
		}

		// If the claim is already under review, verify that the current staff member holds the
		// active lock
		else if (claim.getClaimStatus() == ClaimStatus.UNDER_REVIEW) {
			if (claim.getReviewedBy() != null && !claim.getReviewedBy().getId().equals(staffUserId)) {
				log.warn("Collision blocked: Internal staff {} tried to review claim {} which is locked by internal staff {}", staffUserId,
						claimId, claim.getReviewedBy().getId());
				throw new BusinessRuleException(
						"This claim file is currently locked and being audited by another internal staff member.");
			}
		}

		// Prevent updates if the claim has already passed the review stage
		else {
			throw new BusinessRuleException(
					"This claim has already been processed beyond the review stage. Status: " + claim.getClaimStatus());
		}

		// --- PHASE 2: RECOMMENDATION ENGINE ---
		// Enforce constraint that internal staff can only submit a recommendation status
		if (dto.getRecommendedStatus() != ClaimStatus.RECOMMENDED_APPROVAL
				&& dto.getRecommendedStatus() != ClaimStatus.RECOMMENDED_REJECTION) {
			throw new InvalidStatusException(
					"Internal staff can only submit RECOMMENDED_APPROVAL or RECOMMENDED_REJECTION statuses.");
		}

		ClaimStatus previousStatus = claim.getClaimStatus();

		// Apply form values and transition the state out of the active review lock
		claim.setClaimStatus(dto.getRecommendedStatus());
		claim.setInternalStaffRemarks(dto.getRemarks());
		claim.setReviewedBy(staffMember); // Maintains accountability for the final recommendation

		Claim updatedClaim = claimRepository.save(claim);

		// Log the final recommendation to the status history ledger
		createHistory(updatedClaim, previousStatus, dto.getRecommendedStatus(), dto.getRemarks(), staffMember);

		log.info("Internal staff {} successfully submitted recommendation ({}) for claim {}", staffUserId,
				dto.getRecommendedStatus(), updatedClaim.getClaimNumber());

		return convertToResponseDTO(updatedClaim);
	}

	@Override
	@Transactional
	public ClaimResponseDTO processFinalDecision(Long claimId, Long adminUserId,
			ClaimFinalDecisionRequestDTO decisionDTO) {
		log.info("Admin {} processing final decision for claim {}", adminUserId, claimId);

		User admin = getUser(adminUserId);

		if (admin.getRole() != Role.ADMIN) {
			throw new AuthorizationException("Only admins can make final claim decisions");
		}

		Claim claim = getClaim(claimId);

		if (claim.getClaimStatus() != ClaimStatus.RECOMMENDED_APPROVAL
				&& claim.getClaimStatus() != ClaimStatus.RECOMMENDED_REJECTION) {

			throw new InvalidStatusException("Claim must be recommended before final decision");
		}

		if (decisionDTO.getFinalDecisionStatus() != ClaimStatus.APPROVED
				&& decisionDTO.getFinalDecisionStatus() != ClaimStatus.REJECTED) {

			throw new InvalidStatusException("Final decision must be APPROVED or REJECTED");
		}

		if (claim.getClaimStatus() == ClaimStatus.RECOMMENDED_APPROVAL
				&& decisionDTO.getFinalDecisionStatus() != ClaimStatus.APPROVED) {

			throw new InvalidStatusException("Recommended approval claims can only be approved");
		}

		if (claim.getClaimStatus() == ClaimStatus.RECOMMENDED_REJECTION
				&& decisionDTO.getFinalDecisionStatus() != ClaimStatus.REJECTED) {

			throw new InvalidStatusException("Recommended rejection claims can only be rejected");
		}

		ClaimStatus previousStatus = claim.getClaimStatus();

		claim.setClaimStatus(decisionDTO.getFinalDecisionStatus());

		claim.setAdminRemarks(decisionDTO.getRemarks());

		Claim updatedClaim = claimRepository.save(claim);

		createHistory(updatedClaim, previousStatus, decisionDTO.getFinalDecisionStatus(), decisionDTO.getRemarks(),
				admin);

		if (updatedClaim.getClaimStatus() == ClaimStatus.REJECTED) {

			log.info("Final claim rejection. Claim {} rejected by admin {}", updatedClaim.getClaimNumber(),
					adminUserId);

		}

		if (updatedClaim.getClaimStatus() == ClaimStatus.APPROVED) {

			log.info("Final claim approval. Claim {} approved by admin {}", updatedClaim.getClaimNumber(), adminUserId);

		}

		return convertToResponseDTO(updatedClaim);
	}

	@Override
	@Transactional(readOnly = true)
	public ClaimResponseDTO getClaimById(Long claimId, Long requesterUserId, String requesterRole) {

		Claim claim = getClaim(claimId);

		// Enforces FR-CLM-005: customers may view only their own claims
		if ("CUSTOMER".equals(requesterRole)
				&& !claim.getPolicy().getCustomer().getUser().getId().equals(requesterUserId)) {
			log.warn("Blocked attempt by user {} to view another customer's claim: {}", requesterUserId,
					claim.getClaimNumber());
			throw new AuthorizationException("You are not authorized to view this claim");
		}

		return convertToResponseDTO(claim);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<ClaimResponseDTO> getAllClaims(int page, int size, String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<Claim> claimPage = claimRepository.findAll(pageable);

		Page<ClaimResponseDTO> dtoPage = claimPage.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dtoPage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<ClaimResponseDTO> getClaimsByCustomerId(Long customerId, int page, int size,
			String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<Claim> claimPage =

				claimRepository.findByPolicy_Customer_Id(

						customerId, pageable);

		Page<ClaimResponseDTO> dtoPage = claimPage.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dtoPage, sortBy, direction);
	}



	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<ClaimResponseDTO> getClaimsByStatus(ClaimStatus status, int page, int size,
			String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<Claim> claimPage = claimRepository.findByClaimStatus(status, pageable);

		Page<ClaimResponseDTO> dtoPage = claimPage.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dtoPage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<ClaimResponseDTO> getClaimsByCustomerAndStatus(Long customerId, ClaimStatus status,
			int page, int size, String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<Claim> claimPage = claimRepository

				.findByPolicy_Customer_IdAndClaimStatus(

						customerId, status, pageable);

		Page<ClaimResponseDTO> dtoPage = claimPage.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dtoPage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<ClaimResponseDTO> searchClaimsByNumber(String claimNumber, int page, int size,
			String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);
		Page<Claim> claimPage = claimRepository.findByClaimNumberContainingIgnoreCase(claimNumber, pageable);

		Page<ClaimResponseDTO> dtoPage = claimPage.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dtoPage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<ClaimResponseDTO> getMyClaims(Long userId, int page, int size, String sortBy,
			String direction) {

		Customer customer = customerRepository.findByUser_Id(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<Claim> claimPage = claimRepository.findByPolicy_Customer_Id(customer.getId(), pageable);

		Page<ClaimResponseDTO> dtoPage = claimPage.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dtoPage, sortBy, direction);
	}

	private ClaimResponseDTO convertToResponseDTO(Claim claim) {

		ClaimResponseDTO dto = new ClaimResponseDTO();

		// BASIC CLAIM DETAILS
		dto.setClaimId(claim.getId());

		dto.setClaimNumber(claim.getClaimNumber());

		dto.setClaimAmount(claim.getClaimAmount());

		dto.setClaimReason(claim.getClaimReason());

		dto.setIncidentDate(claim.getIncidentDate());

		dto.setClaimStatus(claim.getClaimStatus());

		dto.setInternalStaffRemarks(claim.getInternalStaffRemarks());

		dto.setAdminRemarks(claim.getAdminRemarks());

		dto.setCreatedDate(claim.getCreatedDate());

		dto.setUpdatedDate(claim.getUpdatedDate());

		Policy policy = claim.getPolicy();

		if (policy != null) {

			// POLICY NUMBER
			dto.setPolicyNumber(policy.getPolicyNumber());

			// CUSTOMER NAME
			if (policy.getCustomer() != null && policy.getCustomer().getUser() != null) {
				dto.setCustomerName(policy.getCustomer().getUser().getFullName());
			}

			// COVERAGE
			BigDecimal coverage = policy.getPolicyPlan().getCoverageAmount();

			dto.setPolicyCoverageAmount(coverage);

			// APPROVED CLAIM TOTAL
			BigDecimal approvedAmount = claimRepository.getApprovedClaimAmount(policy.getId());

			if (approvedAmount == null) {
				approvedAmount = BigDecimal.ZERO;
			}

			dto.setTotalApprovedClaimAmount(approvedAmount);

			// REMAINING
			dto.setRemainingCoverageAmount(coverage.subtract(approvedAmount));

			// PREVIOUS CLAIM COUNT & DETAILED TIMELINE MAP
			List<Claim> previousClaims = claimRepository.findByPolicy_Id(policy.getId(), Pageable.unpaged())
					.getContent();

			dto.setPreviousClaimCount(previousClaims.size());

			// 🕒 DYNAMIC HISTORICAL CLAIMS TIMELINE MAP
			// Filters out the active claim currently being reviewed so only real past
			// history displays
			List<ClaimResponseDTO.PastClaimTimelineDTO> timeline = previousClaims.stream()
					.filter(c -> !c.getId().equals(claim.getId())).map(c -> {
						ClaimResponseDTO.PastClaimTimelineDTO tDto = new ClaimResponseDTO.PastClaimTimelineDTO();
						tDto.setClaimNumber(c.getClaimNumber());
						tDto.setAmount(c.getClaimAmount());
						tDto.setReason(c.getClaimReason());
						tDto.setStatus(c.getClaimStatus().toString());
						tDto.setIncidentDate(c.getIncidentDate());
						return tDto;
					}).toList();

			dto.setPastClaimsTimeline(timeline);
		}

		// REVIEWED BY INTERNAL STAFF
		if (claim.getReviewedBy() != null) {
			dto.setReviewedById(claim.getReviewedBy().getId());
			dto.setReviewedByName(claim.getReviewedBy().getFullName());
		}

		// HISTORY
		if (claim.getClaimStatusHistories() != null) {

			List<ClaimStatusHistoryResponseDTO> historyList = claim.getClaimStatusHistories().stream().map(h -> {

				ClaimStatusHistoryResponseDTO history = new ClaimStatusHistoryResponseDTO();

				history.setHistoryId(h.getId());

				history.setClaimId(claim.getId());

				history.setPreviousStatus(h.getPreviousStatus());

				history.setNewStatus(h.getNewStatus());

				history.setRemarks(h.getRemarks());

				history.setUpdatedDate(h.getUpdatedDate());

				if (h.getUser() != null) {
					history.setUpdatedByFullName(h.getUser().getFullName());
				}

				return history;

			}).toList();

			dto.setHistory(historyList);
		}

		if (claim.getClaimDocuments() != null) {

			List<ClaimDocumentDTO> docs = claim.getClaimDocuments().stream().map(doc -> {
				ClaimDocumentDTO documentDTO = new ClaimDocumentDTO();

				documentDTO.setDocumentId(doc.getId());

				documentDTO.setDocumentName(doc.getDocumentName());

				documentDTO.setDocumentType(doc.getDocumentType());

				documentDTO.setDocumentReference(doc.getDocumentReference());

				return documentDTO;
			}).toList();

			dto.setDocuments(docs);
		}

		return dto;
	}

	@Override
	public PaginatedResponseDTO<ClaimResponseDTO> getInternalStaffClaims(int page, int size, String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		Page<Claim> claims = claimRepository.findByClaimStatusIn(List.of(ClaimStatus.SUBMITTED,
				ClaimStatus.UNDER_REVIEW, ClaimStatus.RECOMMENDED_APPROVAL, ClaimStatus.RECOMMENDED_REJECTION),
				pageable);

		Page<ClaimResponseDTO> dto = claims.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dto, sortBy, direction);
	}

}