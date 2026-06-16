package com.monocept.project.dto;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRequestDTO {

    @NotNull(message = "Policy reference is required")
    private Long policyId;

    @NotNull(message = "Claim amount is required")
    @DecimalMin(value = "1.0", message = "Claim amount must be greater than zero")
    private BigDecimal claimAmount;

    @NotBlank(message = "Claim reason is required")
    private String claimReason;

    @NotNull(message = "Incident date is required")
    @PastOrPresent(message = "Incident date cannot be in the future")
    private LocalDate incidentDate;


    @NotNull(message = "Supporting document details are required")
    private List<ClaimDocumentDTO> supportingDocuments;
}
