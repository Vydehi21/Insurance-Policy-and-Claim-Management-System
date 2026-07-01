package com.monocept.project.service;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyPlanRequestDTO;
import com.monocept.project.dto.PolicyPlanResponseDTO;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.InsuranceProduct;
import com.monocept.project.model.PolicyPlan;
import com.monocept.project.repository.InsuranceProductRepository;
import com.monocept.project.repository.PolicyPlanRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyPlanServiceImplementation implements PolicyPlanService {

	private final PolicyPlanRepository policyPlanRepository;
	private final InsuranceProductRepository productRepository;
	private final ModelMapper modelMapper;

	@Override
	@Transactional
	public PolicyPlanResponseDTO createPlan(PolicyPlanRequestDTO planRequestDTO) {

	    log.info("Creating policy plan: {}", planRequestDTO.getPlanName());

	    InsuranceProduct product = findProductById(planRequestDTO.getProductId());


	    PolicyPlan plan = new PolicyPlan();


	    plan.setPlanName(planRequestDTO.getPlanName());

	    plan.setCoverageAmount(planRequestDTO.getCoverageAmount());

	    plan.setPremiumAmount(planRequestDTO.getPremiumAmount());

	    plan.setPremiumType(planRequestDTO.getPremiumType());

	    plan.setDuration(planRequestDTO.getDuration());

	    plan.setTermsAndConditions(planRequestDTO.getTermsAndConditions());

	    plan.setActiveStatus(planRequestDTO.getActiveStatus());


	    plan.setInsuranceProduct(product);


	    PolicyPlan savedPlan = policyPlanRepository.save(plan);


	    log.info(
	        "Policy plan created successfully id: {}",
	        savedPlan.getId()
	    );


	    return modelMapper.map(
	            savedPlan,
	            PolicyPlanResponseDTO.class
	    );
	}

	@Override
	@Transactional(readOnly = true)
	public PolicyPlanResponseDTO getPlanById(Long planId) {
		log.info("Fetching policy plan with id: {}", planId);

		PolicyPlan plan = findPlanById(planId);

		return modelMapper.map(plan, PolicyPlanResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<PolicyPlanResponseDTO> getAllPlans(int page, int size, String sortBy,
			String direction) {
		log.info("Fetching all policy plans");

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<PolicyPlan> plans = policyPlanRepository.findAll(pageable);

		Page<PolicyPlanResponseDTO> responsePage = plans
				.map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByStatus(Boolean activeStatus, int page, int size,
			String sortBy, String direction) {
		log.info("Fetching plans with status: {}", activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<PolicyPlan> plans = policyPlanRepository.findByActiveStatus(activeStatus, pageable);

		Page<PolicyPlanResponseDTO> responsePage = plans
				.map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByProductId(Long productId, int page, int size,
			String sortBy, String direction) {
		log.info("Fetching plans for product id: {}", productId);

		findProductById(productId);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<PolicyPlan> plans = policyPlanRepository.findByInsuranceProductId(productId, pageable);

		Page<PolicyPlanResponseDTO> responsePage = plans
				.map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByProductIdAndStatus(Long productId,
			Boolean activeStatus, int page, int size, String sortBy, String direction) {
		log.info("Fetching plans for product id: {} and status: {}", productId, activeStatus);

		findProductById(productId);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<PolicyPlan> plans = policyPlanRepository.findByInsuranceProductIdAndActiveStatus(productId, activeStatus,
				pageable);

		Page<PolicyPlanResponseDTO> responsePage = plans
				.map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<PolicyPlanResponseDTO> searchPlansByName(String name, int page, int size, String sortBy,
			String direction) {
		log.info("Searching plans by name: {}", name);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<PolicyPlan> plans = policyPlanRepository.findByPlanNameContainingIgnoreCase(name, pageable);

		Page<PolicyPlanResponseDTO> responsePage = plans
				.map(plan -> modelMapper.map(plan, PolicyPlanResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional
	public PolicyPlanResponseDTO updatePlan(Long planId, PolicyPlanRequestDTO planRequestDTO) {
		log.info("Updating policy plan with id: {}", planId);

		PolicyPlan plan = findPlanById(planId);

		InsuranceProduct product = findProductById(planRequestDTO.getProductId());

		plan.setPlanName(planRequestDTO.getPlanName());
		plan.setCoverageAmount(planRequestDTO.getCoverageAmount());
		plan.setPremiumAmount(planRequestDTO.getPremiumAmount());
		plan.setPremiumType(planRequestDTO.getPremiumType());
		plan.setDuration(planRequestDTO.getDuration());
		plan.setTermsAndConditions(planRequestDTO.getTermsAndConditions());
		plan.setActiveStatus(planRequestDTO.getActiveStatus());
		plan.setInsuranceProduct(product);

		PolicyPlan updatedPlan = policyPlanRepository.save(plan);

		log.info("Policy plan updated successfully with id: {}", updatedPlan.getId());

		return modelMapper.map(updatedPlan, PolicyPlanResponseDTO.class);
	}

	@Override
	@Transactional
	public void deactivatePlan(Long planId) {
		log.info("Deactivating policy plan with id: {}", planId);

		PolicyPlan plan = findPlanById(planId);

		plan.setActiveStatus(false);

		policyPlanRepository.save(plan);

		log.info("Policy plan deactivated successfully with id: {}", planId);
	}
	
	@Override
	public void activatePlan(Long planId){

	    PolicyPlan plan = policyPlanRepository
	            .findById(planId)
	            .orElseThrow(() ->
	                    new RuntimeException("Plan not found"));

	    plan.setActiveStatus(true);

	    policyPlanRepository.save(plan);

	}

	private PolicyPlan findPlanById(Long id) {
		return policyPlanRepository.findById(id).orElseThrow(() -> {
			log.warn("Policy plan not found with id: {}", id);
			return new ResourceNotFoundException("Policy plan not found with id: " + id);
		});
	}

	private InsuranceProduct findProductById(Long id) {
		return productRepository.findById(id).orElseThrow(() -> {
			log.warn("Insurance product not found with id: {}", id);
			return new ResourceNotFoundException("Insurance product not found with id: " + id);
		});
	}
}