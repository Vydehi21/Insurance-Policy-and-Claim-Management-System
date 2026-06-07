package com.monocept.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.monocept.project.enums.PolicyStatus;
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
public class PolicyResponseDTO {

    private Long policyId;
    private String policyNumber;
    private String customerName;
    private String planName;
    private ProductType productType;
    private BigDecimal coverageAmount;
    private BigDecimal premiumAmount;
    private PremiumType premiumType;
    private LocalDate startDate;
    private LocalDate endDate;
    private PolicyStatus policyStatus;
    private BigDecimal totalPremiumPaid;
}
