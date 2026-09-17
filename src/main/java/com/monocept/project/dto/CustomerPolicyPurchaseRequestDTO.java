package com.monocept.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.validation.MultipleOf50000;
import com.monocept.project.validation.WholeNumber;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPolicyPurchaseRequestDTO {

    @NotNull(message = "Plan reference is required")
    private Long planId;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Coverage amount is required")
    @WholeNumber
    @MultipleOf50000
    private BigDecimal coverageAmount;

    @NotNull(message = "Payment frequency is required")
    private PremiumType premiumType;
}