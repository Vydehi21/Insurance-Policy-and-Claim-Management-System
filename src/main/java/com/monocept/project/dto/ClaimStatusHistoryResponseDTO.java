package com.monocept.project.dto;

import com.monocept.project.enums.ClaimStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusHistoryResponseDTO {

    private Long historyId;
    private Long claimId;
    private ClaimStatus previousStatus;
    private ClaimStatus newStatus;
    private String remarks;
    private String updatedByFullName;
    private LocalDateTime updatedDate;
}
