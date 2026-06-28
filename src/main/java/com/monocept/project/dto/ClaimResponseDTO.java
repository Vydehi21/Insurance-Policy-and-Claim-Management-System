package com.monocept.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponseDTO {

    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private String customerName;
    private BigDecimal claimAmount;
    private String claimReason;
    private LocalDate incidentDate;
    private ClaimStatus claimStatus;
    private String agentRemarks;
    private String adminRemarks;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String reviewedByName;
    private String decidedByName;

    private List<ClaimDocumentDTO> documents;
    private BigDecimal policyCoverageAmount;

    private BigDecimal totalApprovedClaimAmount;

    private BigDecimal remainingCoverageAmount;

    private Integer previousClaimCount;

    private List<ClaimStatusHistoryResponseDTO> history;
}
