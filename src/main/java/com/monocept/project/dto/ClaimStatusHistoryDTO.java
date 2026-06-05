package com.monocept.project.dto;

import com.monocept.project.enums.*;
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
public class ClaimStatusHistoryDTO {

    private Long historyId;

    @NotNull(message = "Claim ID association is required")
    private Long claimId;

    @NotNull(message = "Previous status state is required")
    private ClaimStatus previousStatus;

    @NotNull(message = "New target status state is required")
    private ClaimStatus newStatus;

    @Size(max = 500, message = "History log remarks must not exceed 500 characters")
    private String remarks;

    @NotNull(message = "Authorizing user account ID is required")
    private Long updatedById;
}
