package com.monocept.project.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.monocept.project.dto.InsuranceProductRequestDTO;
import com.monocept.project.dto.InsuranceProductResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.service.InsuranceProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class InsuranceProductController {

    private final InsuranceProductService productService;

    @PostMapping
    public ResponseEntity<InsuranceProductResponseDTO> createProduct(

            @Valid
            @RequestBody InsuranceProductRequestDTO requestDTO) {

        InsuranceProductResponseDTO response =
                productService.createProduct(requestDTO);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InsuranceProductResponseDTO> getProductById(

            @PathVariable Long productId) {

        return ResponseEntity.ok(

                productService.getProductById(productId)

        );
    }

    @GetMapping
    public ResponseEntity<PaginatedResponseDTO<InsuranceProductResponseDTO>>
    getAllProducts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {

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
    public ResponseEntity<PaginatedResponseDTO<InsuranceProductResponseDTO>>
    getProductsByStatus(

            @PathVariable Boolean activeStatus,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {

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
    public ResponseEntity<InsuranceProductResponseDTO> updateProduct(

            @PathVariable Long productId,

            @Valid
            @RequestBody InsuranceProductRequestDTO requestDTO) {

        return ResponseEntity.ok(

                productService.updateProduct(
                        productId,
                        requestDTO
                )
        );
    }
    
    @PatchMapping("/{productId}/deactivate")
    public ResponseEntity<String> deactivateProduct(
            @PathVariable Long productId) {

        productService.deactivateProduct(productId);

        return ResponseEntity.ok("Product deactivated successfully");
    }
}