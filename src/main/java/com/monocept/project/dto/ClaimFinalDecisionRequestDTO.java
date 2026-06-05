package com.monocept.project.dto;

import com.monocept.project.enums.ClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimFinalDecisionRequestDTO {

    @NotNull(message = "Final decision status is required")
    private ClaimStatus finalDecisionStatus;

    @NotBlank(message = "Remarks are required")
    private String remarks;
}
