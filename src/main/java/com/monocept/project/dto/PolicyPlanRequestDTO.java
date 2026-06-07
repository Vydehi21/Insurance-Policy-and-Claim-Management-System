package com.monocept.project.dto;

import java.math.BigDecimal;

import com.monocept.project.enums.PremiumType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @Size(max = 100, message = "Plan name must not exceed 100 characters")
    private String planName;

    @NotNull(message = "Coverage amount is required")
    @Min(value = 1, message = "Coverage amount must be greater than zero")
    private BigDecimal coverageAmount;

    @NotNull(message = "Premium amount is required")
    @Min(value = 1, message = "Premium amount must be greater than zero")
    private BigDecimal premiumAmount;

    @NotNull(message = "Premium type is required")
    private PremiumType premiumType;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be greater than zero")
    private Integer duration;

    @NotBlank(message = "Terms and conditions are required")
    private String termsAndConditions;

    @NotNull(message = "Active status is required")
    private Boolean activeStatus;
}
