package com.monocept.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusUpdateRequestDTO {

    @NotNull(message = "Active status is required")
    private Boolean activeStatus;

    private String remarks;
}
