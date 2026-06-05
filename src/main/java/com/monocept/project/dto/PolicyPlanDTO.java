package com.monocept.project.dto;

import com.monocept.project.enums.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPlanDTO {

    private Long planId;

    @NotNull(message = "Product ID association is required")
    private Long productId;

    @NotBlank(message = "Plan name is required")
    @Size(max = 100, message = "Plan name must not exceed 100 characters")
    private String planName;

    @NotNull(message = "Coverage amount is required")
    @Min(value = 0, message = "Coverage amount cannot be negative")
    private Double coverageAmount;

    @NotNull(message = "Premium amount is required")
    @Min(value = 0, message = "Premium amount cannot be negative")
    private Double premiumAmount;

    @NotNull(message = "Premium type is required")
    private PremiumType premiumType;

    @NotNull(message = "Plan duration is required")
    @Min(value = 1, message = "Duration must be at least 1 year")
    private Integer duration;

    private String termsAndConditions;
    private Boolean activeStatus;
}
