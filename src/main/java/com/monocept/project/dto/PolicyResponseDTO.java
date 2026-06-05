package com.monocept.project.dto;

import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.enums.PremiumType;
import com.monocept.project.enums.ProductType;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
    private Double coverageAmount;
    private Double premiumAmount;
    private PremiumType premiumType;
    private LocalDate startDate;
    private LocalDate endDate;
    private PolicyStatus policyStatus;
    private Double totalPremiumPaid;
}
