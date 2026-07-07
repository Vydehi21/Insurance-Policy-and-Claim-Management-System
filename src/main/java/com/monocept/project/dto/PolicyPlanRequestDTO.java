package com.monocept.project.dto;

import java.math.BigDecimal;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.util.TextNormalizationUtil;

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

    @NotNull(message = "Coverage amount is required")
    @DecimalMin(value = "1.0", message = "Coverage amount must be greater than zero")
    @DecimalMax(value = "999999999.99", message = "Coverage amount is unrealistically high")
    private BigDecimal coverageAmount;

    @NotNull(message = "Premium amount is required")
    @DecimalMin(value = "1.0", message = "Premium amount must be greater than zero")
    @DecimalMax(value = "999999999.99", message = "Premium amount is unrealistically high")
    private BigDecimal premiumAmount;

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
}