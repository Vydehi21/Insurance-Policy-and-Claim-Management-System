package com.monocept.project.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for the premium quote endpoint. For MONTHLY/QUARTERLY,
 * discountPercent and discountAmount are always zero (kept in the shape
 * anyway so the frontend doesn't need to special-case those types).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumQuoteResponseDTO {

    private BigDecimal annualPremium;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal finalPremium;
}