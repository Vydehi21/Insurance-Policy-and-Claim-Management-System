package com.monocept.project.repository;

import com.monocept.project.model.InsuranceProduct;
import com.monocept.project.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    Page<InsuranceProduct> findByProductType(ProductType productType, Pageable pageable);
    Page<InsuranceProduct> findByActiveStatusTrue(Pageable pageable);
    Page<InsuranceProduct> findByProductNameContainingIgnoreCase(String productName, Pageable pageable);
}
