package com.monocept.project.repository;

import com.monocept.project.model.InsuranceProduct;
import com.monocept.project.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    List<InsuranceProduct> findByProductType(ProductType productType);
    List<InsuranceProduct> findByActiveStatusTrue();
}
