package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.monocept.project.dto.InsuranceProductRequestDTO;
import com.monocept.project.dto.InsuranceProductResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.service.InsuranceProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173/")
@Tag(name = "Insurance Products", description = "Operations for managing structural definitions of insurance catalogs and products")
public class InsuranceProductController {

    private final InsuranceProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Product", description = "Defines and deploys a brand new structural category classification profile inside the catalog system")
    public ResponseEntity<InsuranceProductResponseDTO> createProduct(
            @Valid @RequestBody InsuranceProductRequestDTO requestDTO) {

        InsuranceProductResponseDTO response =
                productService.createProduct(requestDTO);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Get Product By ID", description = "Fetches comprehensive descriptive configuration states for a specific item identifier mapping")
    public ResponseEntity<InsuranceProductResponseDTO> getProductById(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                productService.getProductById(productId)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Get All Products", description = "Retrieves an indexed pagination data payload matching active active classification catalogs system-wide")
    public ResponseEntity<PaginatedResponseDTO<InsuranceProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                productService.getAllProducts(
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @GetMapping("/status/{activeStatus}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get Products By Status", description = "Filters the catalog indexes by active or dormant lifecycle states")
    public ResponseEntity<PaginatedResponseDTO<InsuranceProductResponseDTO>> getProductsByStatus(
            @PathVariable Boolean activeStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                productService.getProductsByStatus(
                        activeStatus,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Product", description = "Modifies active properties and attributes tracking inside existing catalog entries")
    public ResponseEntity<InsuranceProductResponseDTO> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody InsuranceProductRequestDTO requestDTO) {

        return ResponseEntity.ok(
                productService.updateProduct(
                        productId,
                        requestDTO
                )
        );
    }
    
    @PatchMapping("/{productId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate Product", description = "Transitions an operational entity model state straight into a dormant classification status")
    public ResponseEntity<String> deactivateProduct(
            @PathVariable Long productId) {

        productService.deactivateProduct(productId);
        return ResponseEntity.ok("Product deactivated successfully");
    }
    
    @PatchMapping("/{productId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Activate Product",
        description = "Activates an inactive insurance product"
    )
    public ResponseEntity<String> activateProduct(
            @PathVariable Long productId) {

        productService.activateProduct(productId);

        return ResponseEntity.ok("Product activated successfully");
    }
}
