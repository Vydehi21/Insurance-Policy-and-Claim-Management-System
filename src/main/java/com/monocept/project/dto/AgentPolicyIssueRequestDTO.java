package com.monocept.project.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentPolicyIssueRequestDTO {

    @NotNull(message = "Customer reference is required")
    private Long customerId;

    @NotNull(message = "Plan reference is required")
    private Long planId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
}
