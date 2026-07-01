package com.monocept.project.service;

import com.monocept.project.dto.PolicyPlanRequestDTO;
import com.monocept.project.dto.PolicyPlanResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;

public interface PolicyPlanService {
    PolicyPlanResponseDTO createPlan(PolicyPlanRequestDTO planRequestDTO);
    PolicyPlanResponseDTO getPlanById(Long planId);
    PaginatedResponseDTO<PolicyPlanResponseDTO> getAllPlans(int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByStatus(Boolean activeStatus, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByProductId(Long productId, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyPlanResponseDTO> getPlansByProductIdAndStatus(Long productId, Boolean activeStatus, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<PolicyPlanResponseDTO> searchPlansByName(String name, int page, int size, String sortBy, String direction);
    PolicyPlanResponseDTO updatePlan(Long planId, PolicyPlanRequestDTO planRequestDTO);
    void deactivatePlan(Long planId);
    void activatePlan(Long planId);
}
