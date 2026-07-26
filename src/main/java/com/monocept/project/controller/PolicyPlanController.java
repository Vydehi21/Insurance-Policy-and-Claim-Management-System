package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyPlanRequestDTO;
import com.monocept.project.dto.PolicyPlanResponseDTO;
import com.monocept.project.service.PolicyPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Policy Plans", description = "Operations for configuring and managing specific insurance policy plans")
public class PolicyPlanController {

    private final PolicyPlanService planService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Plan", description = "Establishes a new insurance tier or scheme option within the configuration catalog")
    public ResponseEntity<PolicyPlanResponseDTO> createPlan(
            @Valid @RequestBody PolicyPlanRequestDTO requestDTO) {

        PolicyPlanResponseDTO response =
                planService.createPlan(requestDTO);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL_STAFF', 'CUSTOMER')")
    @Operation(summary = "Get Plan By ID", description = "Fetches the full specification data of a policy plan via its primary key")
    public ResponseEntity<PolicyPlanResponseDTO> getPlanById(
            @PathVariable Long planId) {

        return ResponseEntity.ok(
                planService.getPlanById(planId)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL_STAFF', 'CUSTOMER')")
    @Operation(summary = "Get All Plans", description = "Returns a paginated list tracking every active and structural plan variant system-wide, optionally filtered by name search and/or active status")
    public ResponseEntity<PaginatedResponseDTO<PolicyPlanResponseDTO>> getAllPlans(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        PaginatedResponseDTO<PolicyPlanResponseDTO> response;

        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && status != null) {
            response = planService.searchPlansByNameAndStatus(search, status, page, size, sortBy, direction);
        } else if (hasSearch) {
            response = planService.searchPlansByName(search, page, size, sortBy, direction);
        } else if (status != null) {
            response = planService.getPlansByStatus(status, page, size, sortBy, direction);
        } else {
            response = planService.getAllPlans(page, size, sortBy, direction);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL_STAFF', 'CUSTOMER')")
    @Operation(summary = "Get Plans By Product ID", description = "Filters the plan schemas to return configurations nested under a common insurance product type")
    public ResponseEntity<PaginatedResponseDTO<PolicyPlanResponseDTO>> getPlansByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                planService.getPlansByProductId(
                        productId,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @PutMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Plan", description = "Modifies existing plan rates, durations, or terms based on payload properties")
    public ResponseEntity<PolicyPlanResponseDTO> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody PolicyPlanRequestDTO requestDTO) {

        return ResponseEntity.ok(
                planService.updatePlan(
                        planId,
                        requestDTO
                )
        );
    }

    @PatchMapping("/{planId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate Plan", description = "Sets an operational policy scheme to a dormant state to restrict new purchases")
    public ResponseEntity<String> deactivatePlan(
            @PathVariable Long planId) {

        planService.deactivatePlan(planId);
        return ResponseEntity.ok("Policy plan deactivated successfully");
    }
    
    @PatchMapping("/{planId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Activate Plan",
        description = "Activates a previously deactivated policy plan"
    )
    public ResponseEntity<String> activatePlan(
            @PathVariable Long planId){

        planService.activatePlan(planId);

        return ResponseEntity.ok("Policy plan activated successfully");
    }
}