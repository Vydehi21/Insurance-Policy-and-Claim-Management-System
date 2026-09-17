package com.monocept.project.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
	private final EmailService emailService;

	@Value("${cloudinary.cloud-name}")
	private String cloudinaryCloudName;

	private static final String CLOUDINARY_HOST = "res.cloudinary.com";

	// A review lock not refreshed within this window can be taken over.
	private static final int REVIEW_LOCK_MINUTES = 30;

	// Days after a missed recurring premium before claims are blocked.
	private static final int PREMIUM_GRACE_DAYS = 30;

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
	 * reachable file hosted on this project's Cloudinary account AND uploaded
	 * by this customer (uploads go to claims/user-{id}/), not just an
	 * arbitrary string or another customer's file URL.
	 */
	private void validateSupportingDocuments(List<ClaimDocumentDTO> documentDTOs, Long customerUserId) {

		if (documentDTOs == null || documentDTOs.isEmpty()) {
			throw new InvalidRequestException(
					"At least one supporting document must be submitted to raise a claim");
		}

		for (ClaimDocumentDTO documentDTO : documentDTOs) {
			validateDocumentReference(documentDTO.getDocumentReference(), customerUserId);
		}
	}

	private void validateDocumentReference(String documentReference, Long customerUserId) {

		URI uri;

		try {
			uri = URI.create(documentReference);
		} catch (Exception e) {
			throw new InvalidRequestException("Supporting document reference is not a valid URL");
		}

		String rawPath = uri.getPath();
		String normalizedPath = uri.normalize().getPath();

		// STEP 1: Domain allowlist check (also the SSRF guard for step 2). Paths
		// containing ".." are rejected so "/<cloud>/../other-cloud/..." can't
		// pass the prefix check.
		boolean isTrustedCloudinaryUrl = "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
				&& uri.getHost().equalsIgnoreCase(CLOUDINARY_HOST) && rawPath != null
				&& rawPath.equals(normalizedPath) && !rawPath.contains("..")
				&& rawPath.startsWith("/" + cloudinaryCloudName + "/");

		if (!isTrustedCloudinaryUrl) {
			log.warn("Rejected claim document with untrusted or malformed reference");
			throw new InvalidRequestException(
					"Supporting document must be a valid file uploaded via /api/files/upload");
		}

		// STEP 2: Ownership. /api/files/upload stores every file under the
		// uploader's own folder, so a URL outside it belongs to someone else.
		String ownFolder = "/" + CloudinaryService.claimFolderFor(customerUserId) + "/";
		if (!rawPath.contains(ownFolder)) {
			log.warn("LOG-014 Rejected claim document not uploaded by customer user {}", customerUserId);
			throw new InvalidRequestException(
					"Supporting documents must be files you uploaded yourself. Please upload the document again.");
		}

		// STEP 3: Live reachability check. Confirms the file actually exists
		// at Cloudinary instead of just looking like a plausible URL.
		try {
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

			HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(5))
					.method("HEAD", HttpRequest.BodyPublishers.noBody()).build();

			HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

			if (response.statusCode() != 200) {
				log.warn("Document verification failed. Cloudinary returned status {}", response.statusCode());
				throw new InvalidRequestException(
						"Supporting document could not be verified (file not found at the provided URL). Please re-upload and try again.");
			}

		} catch (InvalidRequestException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error while verifying supporting document reference", e);
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

		Claim claim = claimRepository.findByIdForUpdate(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim record not found"));

		User currentStaff = getUser(staffUserId);

		boolean lockedByOther = isLockedByAnotherStaff(claim, staffUserId);

		// Opening a claim takes (or refreshes) the review lock, unless another
		// staff member holds a live lock. Previously a lock never expired and a
		// second staff member couldn't even view the claim.
		if (!lockedByOther && (claim.getClaimStatus() == ClaimStatus.SUBMITTED
				|| claim.getClaimStatus() == ClaimStatus.UNDER_REVIEW)) {
			claim = acquireReviewLock(claim, currentStaff);
		}

		if (lockedByOther) {
			log.info("Claim {} opened read-only by staff {}: locked by staff {}", claim.getClaimNumber(),
					staffUserId, claim.getReviewedBy().getId());
		}

		ClaimResponseDTO dto = convertToResponseDTO(claim);
		dto.setLockedByAnotherStaff(lockedByOther);
		if (claim.getClaimStatus() == ClaimStatus.UNDER_REVIEW && claim.getReviewLockedAt() != null) {
			dto.setReviewLockExpiresAt(claim.getReviewLockedAt().plusMinutes(REVIEW_LOCK_MINUTES));
		}
		return dto;
	}

	/**
	 * A lock is live only while its holder is still active and it was taken or
	 * refreshed within REVIEW_LOCK_MINUTES. Locks from before this change (no
	 * timestamp) count as expired so stuck claims can be picked up again.
	 */
	private boolean isLockedByAnotherStaff(Claim claim, Long staffUserId) {
		if (claim.getClaimStatus() != ClaimStatus.UNDER_REVIEW) {
			return false;
		}
		User holder = claim.getReviewedBy();
		if (holder == null || holder.getId().equals(staffUserId)) {
			return false;
		}
		if (!Boolean.TRUE.equals(holder.getActiveStatus())) {
			return false;
		}
		return claim.getReviewLockedAt() != null
				&& claim.getReviewLockedAt().plusMinutes(REVIEW_LOCK_MINUTES).isAfter(LocalDateTime.now());
	}

	private Claim acquireReviewLock(Claim claim, User staff) {
		ClaimStatus previousStatus = claim.getClaimStatus();
		boolean newHolder = claim.getReviewedBy() == null || !claim.getReviewedBy().getId().equals(staff.getId());

		claim.setClaimStatus(ClaimStatus.UNDER_REVIEW);
		claim.setReviewedBy(staff);
		claim.setReviewLockedAt(LocalDateTime.now());
		Claim saved = claimRepository.save(claim);

		if (previousStatus == ClaimStatus.SUBMITTED) {
			createHistory(saved, previousStatus, ClaimStatus.UNDER_REVIEW,
					"Claim locked for inspection by internal staff.", staff);
			log.info("LOG-010 Claim {} taken for review by staff {}", saved.getClaimNumber(), staff.getId());
		} else if (newHolder) {
			createHistory(saved, previousStatus, ClaimStatus.UNDER_REVIEW,
					"Previous review lock expired; review taken over by another internal staff member.", staff);
			log.info("LOG-010 Claim {} review lock taken over by staff {}", saved.getClaimNumber(), staff.getId());
		}
		return saved;
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

		// A recurring policy whose premium is overdue beyond the grace period
		// can't be claimed on. Before, a missed monthly premium had no effect.
		if (policy.getNextPremiumDueDate() != null
				&& LocalDate.now().isAfter(policy.getNextPremiumDueDate().plusDays(PREMIUM_GRACE_DAYS))) {
			log.warn("LOG-015 Business rule violation. Claim attempted on policy {} with premium overdue since {}",
					policy.getPolicyNumber(), policy.getNextPremiumDueDate());
			throw new BusinessRuleException("The premium for this policy has been overdue since "
					+ policy.getNextPremiumDueDate() + ". Please pay the pending premium before raising a claim.");
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

		if (totalClaimAmount.compareTo(policy.getCoverageAmount()) > 0) {
			log.warn("Business rule violation. Claim amount {} (cumulative {}) exceeds coverage {} for policy {}",
					dto.getClaimAmount(), totalClaimAmount, policy.getCoverageAmount(),
					policy.getPolicyNumber());
			throw new BusinessRuleException("Claim exceeds remaining coverage amount");
		}

		// Blocks a new claim while any prior claim on this policy is still "in flight" —
		// i.e. submitted, under agent review, or already recommended one way or the
		// other but not yet given a final decision by admin. Only APPROVED/REJECTED
		// (final, closed) claims don't block a new submission.
		boolean exists = claimRepository.existsByPolicyIdAndClaimStatusIn(policy.getId(),
				List.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW,
						ClaimStatus.RECOMMENDED_APPROVAL, ClaimStatus.RECOMMENDED_REJECTION));

		if (exists) {
			log.warn("Business rule violation. Duplicate open claim attempted for policy: {}",
					policy.getPolicyNumber());
			throw new BusinessRuleException("A claim already exists for this policy");
		}

		validateSupportingDocuments(dto.getSupportingDocuments(), authenticatedUserId);

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
		
		emailService.sendClaimSubmittedEmail(customer.getEmail(), customer.getFullName(),
				savedClaim.getClaimNumber(), savedClaim.getClaimAmount());

		log.info("LOG-009 Claim {} submitted successfully", savedClaim.getClaimNumber());

		return convertToResponseDTO(savedClaim);
	}

	@Override
	@Transactional
	public ClaimResponseDTO reviewClaim(Long claimId, Long staffUserId, ClaimReviewRequestDTO dto) {

		log.info("Internal staff user {} is submitting a recommendation for claim ID {}", staffUserId, claimId);

		// Staff can only recommend; the final decision belongs to admin.
		if (dto.getRecommendedStatus() != ClaimStatus.RECOMMENDED_APPROVAL
				&& dto.getRecommendedStatus() != ClaimStatus.RECOMMENDED_REJECTION) {
			throw new InvalidStatusException(
					"Internal staff can only submit RECOMMENDED_APPROVAL or RECOMMENDED_REJECTION statuses.");
		}

		Claim claim = claimRepository.findByIdForUpdate(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim file reference not found"));

		User staffMember = getUser(staffUserId);

		if (claim.getClaimStatus() != ClaimStatus.SUBMITTED && claim.getClaimStatus() != ClaimStatus.UNDER_REVIEW) {
			throw new BusinessRuleException(
					"This claim has already been processed beyond the review stage. Status: " + claim.getClaimStatus());
		}

		if (isLockedByAnotherStaff(claim, staffUserId)) {
			log.warn("Collision blocked: staff {} tried to review claim {} locked by staff {}", staffUserId,
					claimId, claim.getReviewedBy().getId());
			throw new BusinessRuleException("This claim is currently being reviewed by "
					+ claim.getReviewedBy().getFullName() + ". Please try again later.");
		}

		claim = acquireReviewLock(claim, staffMember);

		ClaimStatus previousStatus = claim.getClaimStatus();

		claim.setClaimStatus(dto.getRecommendedStatus());
		claim.setInternalStaffRemarks(dto.getRemarks());
		claim.setReviewedBy(staffMember);
		// Recommendation submitted: the review lock is no longer needed.
		claim.setReviewLockedAt(null);

		Claim updatedClaim = claimRepository.save(claim);

		createHistory(updatedClaim, previousStatus, dto.getRecommendedStatus(), dto.getRemarks(), staffMember);

		User claimCustomer = updatedClaim.getPolicy().getCustomer().getUser();
		emailService.sendClaimReviewedEmail(claimCustomer.getEmail(), claimCustomer.getFullName(),
				updatedClaim.getClaimNumber(), dto.getRecommendedStatus().name(), dto.getRemarks());

		log.info("LOG-011 Staff {} recommended {} for claim {}", staffUserId,
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

		Claim claim = claimRepository.findByIdForUpdate(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		if (claim.getClaimStatus() == ClaimStatus.APPROVED || claim.getClaimStatus() == ClaimStatus.REJECTED) {
			throw new InvalidStatusException("Approved or rejected claims cannot be modified");
		}

		if (claim.getClaimStatus() != ClaimStatus.RECOMMENDED_APPROVAL
				&& claim.getClaimStatus() != ClaimStatus.RECOMMENDED_REJECTION) {
			throw new InvalidStatusException("Claim must be recommended by internal staff before final decision");
		}

		if (decisionDTO.getFinalDecisionStatus() != ClaimStatus.APPROVED
				&& decisionDTO.getFinalDecisionStatus() != ClaimStatus.REJECTED) {
			throw new InvalidStatusException("Final decision must be APPROVED or REJECTED");
		}

		// SRS: staff only recommend, admin makes the final decision — so admin may
		// decide against the recommendation (remarks are mandatory). Previously
		// the admin was forced to follow the staff recommendation.
		boolean overridesRecommendation =
				(claim.getClaimStatus() == ClaimStatus.RECOMMENDED_APPROVAL
						&& decisionDTO.getFinalDecisionStatus() == ClaimStatus.REJECTED)
				|| (claim.getClaimStatus() == ClaimStatus.RECOMMENDED_REJECTION
						&& decisionDTO.getFinalDecisionStatus() == ClaimStatus.APPROVED);

		if (decisionDTO.getFinalDecisionStatus() == ClaimStatus.APPROVED) {
			Policy policy = claim.getPolicy();

			if (policy.getPolicyStatus() == PolicyStatus.CANCELLED) {
				throw new BusinessRuleException("This policy has been cancelled, so the claim can't be approved.");
			}

			// Re-check coverage at approval time (CLM-BR-004).
			BigDecimal alreadyApproved = claimRepository.getApprovedClaimAmount(policy.getId());
			if (alreadyApproved == null) {
				alreadyApproved = BigDecimal.ZERO;
			}
			if (alreadyApproved.add(claim.getClaimAmount()).compareTo(policy.getCoverageAmount()) > 0) {
				throw new BusinessRuleException("Approving this claim would exceed the policy's remaining coverage of "
						+ policy.getCoverageAmount().subtract(alreadyApproved));
			}
		}

		ClaimStatus previousStatus = claim.getClaimStatus();

		claim.setClaimStatus(decisionDTO.getFinalDecisionStatus());
		claim.setAdminRemarks(decisionDTO.getRemarks());
		claim.setDecidedBy(admin);

		Claim updatedClaim = claimRepository.save(claim);

		createHistory(updatedClaim, previousStatus, decisionDTO.getFinalDecisionStatus(), decisionDTO.getRemarks(),
				admin);

		User decisionCustomer = updatedClaim.getPolicy().getCustomer().getUser();
		emailService.sendClaimDecisionEmail(decisionCustomer.getEmail(), decisionCustomer.getFullName(),
				updatedClaim.getClaimNumber(), decisionDTO.getFinalDecisionStatus().name(), decisionDTO.getRemarks());

		if (overridesRecommendation) {
			log.info("Admin {} decided {} on claim {} against the staff recommendation {}", adminUserId,
					updatedClaim.getClaimStatus(), updatedClaim.getClaimNumber(), previousStatus);
		}

		if (updatedClaim.getClaimStatus() == ClaimStatus.REJECTED) {
			log.info("LOG-013 Final claim rejection. Claim {} rejected by admin {}", updatedClaim.getClaimNumber(),
					adminUserId);
		}

		if (updatedClaim.getClaimStatus() == ClaimStatus.APPROVED) {
			log.info("LOG-012 Final claim approval. Claim {} approved by admin {}", updatedClaim.getClaimNumber(), adminUserId);
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
			BigDecimal coverage = policy.getCoverageAmount();

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

		if (claim.getDecidedBy() != null) {
			dto.setDecidedByName(claim.getDecidedBy().getFullName());
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

		// CHANGED: previously restricted to SUBMITTED/UNDER_REVIEW/
		// RECOMMENDED_APPROVAL/RECOMMENDED_REJECTION only, which meant the
		// frontend's "All Statuses", "Approved", and "Rejected" filter
		// options on this same dataset could never show anything — those
		// claims were never fetched in the first place. Staff need to see a
		// claim's full lifecycle (including the eventual APPROVED/REJECTED/
		// CANCELLED outcome) for audit purposes; the "Needs Action" default
		// view on the frontend already narrows this down to SUBMITTED/
		// UNDER_REVIEW for the actionable queue.
		Page<Claim> claims = claimRepository.findAll(pageable);

		Page<ClaimResponseDTO> dto = claims.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dto, sortBy, direction);
	}
	
    @Override
    @Transactional
    public void withdrawClaimByCustomer(Long claimId, Long authenticatedUserId) {
        log.info("Customer user ID {} is attempting to withdraw claim ID {}", authenticatedUserId, claimId);

        // 1. Locate the claim ensuring the customer actually owns the parent policy file
        Claim claim = claimRepository.findByIdAndPolicy_Customer_User_Id(claimId, authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim file reference not found or access denied"));

        // 2. Enforce the state constraint rule: Cannot withdraw once advanced beyond review
        if (claim.getClaimStatus() != ClaimStatus.SUBMITTED && claim.getClaimStatus() != ClaimStatus.UNDER_REVIEW) {
            log.warn("Withdrawal blocked: Claim {} is already in status {}", claimId, claim.getClaimStatus());
            throw new BusinessRuleException("Cannot withdraw this claim because it has already been processed.");
        }

        ClaimStatus previousStatus = claim.getClaimStatus();
        User customerUser = claim.getPolicy().getCustomer().getUser();

        // 3. Clear the status and release any active agent locks
        claim.setClaimStatus(ClaimStatus.CANCELLED); // or ClaimStatus.WITHDRAWN based on your exact enum
        claim.setReviewedBy(null); // Safely releases any active agent concurrency locks
        claim.setReviewLockedAt(null);
        claim.setInternalStaffRemarks("Withdrawn by customer.");

        Claim savedClaim = claimRepository.save(claim);

        // 4. Log the action to your status tracking history ledger
        createHistory(
            savedClaim,
            previousStatus,
            ClaimStatus.CANCELLED,
            "Claim voluntarily withdrawn by customer.",
            customerUser
        );

        emailService.sendClaimWithdrawnEmail(customerUser.getEmail(), customerUser.getFullName(),
                savedClaim.getClaimNumber());

        log.info("Claim {} successfully withdrawn and cancelled by customer {}", savedClaim.getClaimNumber(), authenticatedUserId);
    }


	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<ClaimResponseDTO> getClaimsPendingAdminDecision(int page, int size, String sortBy, String direction) {

		Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, direction);

		// CLC-RUL-004 / SRS §7.1: admin's authority is the FINAL decision only, so
		// the admin claims queue must default-exclude SUBMITTED claims that have
		// not yet been picked up and reviewed by an internal staff member.
		// Already-decided claims (APPROVED/REJECTED) are still included so admin
		// has a full audit trail, but the frontend renders those read-only per
		// CLM-BR-009 (approved/rejected claims cannot be modified again).
		Page<Claim> claimPage = claimRepository.findByClaimStatusIn(
				List.of(
						ClaimStatus.UNDER_REVIEW,
						ClaimStatus.RECOMMENDED_APPROVAL,
						ClaimStatus.RECOMMENDED_REJECTION,
						ClaimStatus.APPROVED,
						ClaimStatus.REJECTED),
				pageable);

		Page<ClaimResponseDTO> dtoPage = claimPage.map(this::convertToResponseDTO);

		return PaginationUtil.createPaginatedResponse(dtoPage, sortBy, direction);
	}

}