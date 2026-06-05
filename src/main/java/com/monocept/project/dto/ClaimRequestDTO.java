package com.monocept.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRequestDTO {

    @NotNull(message = "Policy reference is required")
    private Long policyId;

    @NotNull(message = "Claim amount is required")
    @Min(value = 1, message = "Claim amount must be greater than zero")
    private Double claimAmount;

    @NotBlank(message = "Claim reason is required")
    private String claimReason;

    @NotNull(message = "Incident date is required")
    private LocalDate incidentDate;

    @NotNull(message = "Supporting document details are required")
    private List<ClaimDocumentDTO> supportingDocuments;
}
