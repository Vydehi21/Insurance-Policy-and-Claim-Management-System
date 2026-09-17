package com.monocept.project.dto;

import java.math.BigDecimal;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.enums.ProductType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPlanResponseDTO {

    private Long planId;
    private Long productId;
    private String productName;
    private ProductType productType;
    private String planName;
    private BigDecimal minCoverageAmount;
    private BigDecimal maxCoverageAmount;
    private BigDecimal ratePerUnit;
    private BigDecimal annualDiscountPercent;
    private BigDecimal oneTimeDiscountPercent;
    private PremiumType premiumType;
    private Integer duration;
    private String termsAndConditions;
    private Boolean activeStatus;
}