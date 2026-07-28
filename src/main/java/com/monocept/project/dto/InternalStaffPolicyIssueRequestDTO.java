package com.monocept.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.validation.MultipleOf50000;
import com.monocept.project.validation.WholeNumber;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalStaffPolicyIssueRequestDTO {

    @NotNull(message = "Customer reference is required")
    private Long customerId;

    @NotNull(message = "Plan reference is required")
    private Long planId;

    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    // Optional: defaults to the plan's max coverage if not provided.
    @WholeNumber
    @MultipleOf50000
    private BigDecimal coverageAmount;

    // Optional: defaults to the plan's own premiumType if not provided.
    private PremiumType premiumType;
}