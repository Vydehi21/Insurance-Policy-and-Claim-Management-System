package com.monocept.project.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.PolicyPlanRequestDTO;
import com.monocept.project.dto.PolicyPlanResponseDTO;
import com.monocept.project.service.PolicyPlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PolicyPlanController {

    private final PolicyPlanService planService;

    @PostMapping
    public ResponseEntity<PolicyPlanResponseDTO> createPlan(

            @Valid
            @RequestBody PolicyPlanRequestDTO requestDTO) {

        PolicyPlanResponseDTO response =
                planService.createPlan(requestDTO);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PolicyPlanResponseDTO> getPlanById(

            @PathVariable Long planId) {

        return ResponseEntity.ok(
                planService.getPlanById(planId)
        );
    }

    @GetMapping
    public ResponseEntity<PaginatedResponseDTO<PolicyPlanResponseDTO>>
    getAllPlans(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {

        return ResponseEntity.ok(

                planService.getAllPlans(
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PaginatedResponseDTO<PolicyPlanResponseDTO>>
    getPlansByProduct(

            @PathVariable Long productId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {

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
    public ResponseEntity<PolicyPlanResponseDTO> updatePlan(

            @PathVariable Long planId,

            @Valid
            @RequestBody PolicyPlanRequestDTO requestDTO) {

        return ResponseEntity.ok(

                planService.updatePlan(
                        planId,
                        requestDTO
                )
        );
    }

    @PatchMapping("/{planId}/deactivate")
    public ResponseEntity<String> deactivatePlan(

            @PathVariable Long planId) {

        planService.deactivatePlan(planId);

        return ResponseEntity.ok("Policy plan deactivated successfully");
    }



}