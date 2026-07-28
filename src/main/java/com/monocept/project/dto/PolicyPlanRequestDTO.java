package com.monocept.project.dto;

import java.math.BigDecimal;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.util.TextNormalizationUtil;
import com.monocept.project.validation.MultipleOf50000;
import com.monocept.project.validation.WholeNumber;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPlanRequestDTO {

    @NotNull(message = "Product reference ID is required")
    private Long productId;

    @NotBlank(message = "Plan name is required")
    @Size(min = 3, max = 100, message = "Plan name must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "Plan name can only contain letters, numbers and spaces")
    private String planName;

    // Minimum coverage a customer may choose at purchase time.
    @NotNull(message = "Minimum coverage amount is required")
    @DecimalMin(value = "50000", message = "Minimum coverage amount must be at least 50,000")
    @WholeNumber
    @MultipleOf50000
    private BigDecimal minCoverageAmount;

    // Maximum coverage a customer may choose at purchase time.
    @NotNull(message = "Maximum coverage amount is required")
    @DecimalMin(value = "50000", message = "Maximum coverage amount must be at least 50,000")
    @DecimalMax(value = "999999999.99", message = "Maximum coverage amount is unrealistically high")
    @WholeNumber
    @MultipleOf50000
    private BigDecimal maxCoverageAmount;

    // Premium charged per ₹50,000 of coverage, per year.
    @NotNull(message = "Rate per unit is required")
    @DecimalMin(value = "0.01", message = "Rate per unit must be greater than zero")
    private BigDecimal ratePerUnit;

    @NotNull(message = "Annual discount percent is required")
    @DecimalMin(value = "0", message = "Annual discount percent cannot be negative")
    @DecimalMax(value = "100", message = "Annual discount percent cannot exceed 100")
    private BigDecimal annualDiscountPercent = BigDecimal.ZERO;

    @NotNull(message = "One-time discount percent is required")
    @DecimalMin(value = "0", message = "One-time discount percent cannot be negative")
    @DecimalMax(value = "100", message = "One-time discount percent cannot exceed 100")
    private BigDecimal oneTimeDiscountPercent = BigDecimal.ZERO;

    @NotNull(message = "Premium type is required")
    private PremiumType premiumType;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 year")
    @jakarta.validation.constraints.Max(value = 50, message = "Duration cannot exceed 50 years")
    private Integer duration;

    @NotBlank(message = "Terms and conditions are required")
    @Size(min = 10, max = 2000, message = "Terms and conditions must be between 10 and 2000 characters")
    @Pattern(regexp = ".*[A-Za-z]{3,}.*", message = "Terms and conditions must contain meaningful text")
    private String termsAndConditions;

    @NotNull(message = "Active status is required")
    private Boolean activeStatus;

    public void setPlanName(String planName) {
        this.planName = TextNormalizationUtil.toTitleCase(planName);
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = TextNormalizationUtil.trimAndCollapseSpaces(termsAndConditions);
    }

    // Bean Validation cross-field check: runs automatically because the
    // method is a getter-shaped boolean (isXxx/getXxx pattern via @AssertTrue).
    @AssertTrue(message = "Maximum coverage amount must be greater than or equal to minimum coverage amount")
    private boolean isCoverageRangeValid() {
        if (minCoverageAmount == null || maxCoverageAmount == null) {
            return true; // let @NotNull report the missing field instead
        }
        return maxCoverageAmount.compareTo(minCoverageAmount) >= 0;
    }
}