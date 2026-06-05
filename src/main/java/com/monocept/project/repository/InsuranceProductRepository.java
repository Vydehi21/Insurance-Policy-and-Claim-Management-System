package com.monocept.project.repository;

import com.monocept.project.model.InsuranceProduct;
import com.monocept.project.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    boolean existsByProductName(String productName);
    Page<InsuranceProduct> findByProductType(ProductType productType, Pageable pageable);
    Page<InsuranceProduct> findByActiveStatus(Boolean activeStatus, Pageable pageable);
    Page<InsuranceProduct> findByProductTypeAndActiveStatus(ProductType productType, Boolean activeStatus, Pageable pageable);
    Page<InsuranceProduct> findByProductNameContainingIgnoreCase(String productName, Pageable pageable);
}
