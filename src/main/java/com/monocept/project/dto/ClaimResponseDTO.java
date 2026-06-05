package com.monocept.project.dto;

import com.monocept.project.enums.ClaimStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponseDTO {

    private Long claimId;
    private String claimNumber;
    private String policyNumber;
    private String customerName;
    private Double claimAmount;
    private String claimReason;
    private LocalDate incidentDate;
    private ClaimStatus claimStatus;
    private String agentRemarks;
    private String adminRemarks;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
