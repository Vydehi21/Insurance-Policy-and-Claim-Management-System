package com.monocept.project.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentPolicyIssueRequestDTO {

    @NotNull(message = "Customer reference is required")
    private Long customerId;

    @NotNull(message = "Plan reference is required")
    private Long planId;

    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;
}
