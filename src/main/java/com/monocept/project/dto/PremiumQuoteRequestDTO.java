package com.monocept.project.dto;

import java.math.BigDecimal;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.validation.MultipleOf50000;
import com.monocept.project.validation.WholeNumber;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for the read-only premium quote endpoint. planId comes from
 * the path, so it isn't repeated here — see PolicyPlanController.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumQuoteRequestDTO {

    @NotNull(message = "Coverage amount is required")
    @WholeNumber
    @MultipleOf50000
    private BigDecimal coverageAmount;

    @NotNull(message = "Premium type is required")
    private PremiumType premiumType;
}