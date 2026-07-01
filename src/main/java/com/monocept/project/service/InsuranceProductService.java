package com.monocept.project.service;

import com.monocept.project.dto.InsuranceProductRequestDTO;
import com.monocept.project.dto.InsuranceProductResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ProductType;

public interface InsuranceProductService {
    InsuranceProductResponseDTO createProduct(InsuranceProductRequestDTO productRequestDTO);
    InsuranceProductResponseDTO getProductById(Long productId);
    PaginatedResponseDTO<InsuranceProductResponseDTO> getAllProducts(int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<InsuranceProductResponseDTO> getProductsByStatus(Boolean activeStatus, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<InsuranceProductResponseDTO> getProductsByType(ProductType productType, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<InsuranceProductResponseDTO> getProductsByTypeAndStatus(ProductType productType, Boolean activeStatus, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<InsuranceProductResponseDTO> searchProductsByName(String name, int page, int size, String sortBy, String direction);
    InsuranceProductResponseDTO updateProduct(Long productId, InsuranceProductRequestDTO productRequestDTO);
    void deactivateProduct(Long productId);
    void activateProduct(Long productId);
}
