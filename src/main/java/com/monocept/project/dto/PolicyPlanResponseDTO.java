package com.monocept.project.dto;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.enums.ProductType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPlanResponseDTO {

    private Long planId;
    private String productName;
    private ProductType productType;
    private String planName;
    private Double coverageAmount;
    private Double premiumAmount;
    private PremiumType premiumType;
    private Integer duration;
    private String termsAndConditions;
    private Boolean activeStatus;
}
