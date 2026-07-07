package com.monocept.project.dto;

import com.monocept.project.enums.ClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReviewRequestDTO {

    @NotNull(message = "Recommended status is required")
    private ClaimStatus recommendedStatus;

    @NotBlank(message = "Remarks are required")
    @Size(min = 3, max = 500, message = "Remarks must be between 3 and 500 characters")
    @Pattern(regexp = ".*[A-Za-z]{2,}.*", message = "Remarks must contain meaningful text")
    private String remarks;

    public void setRemarks(String remarks) {
        this.remarks = com.monocept.project.util.TextNormalizationUtil.trimAndCollapseSpaces(remarks);
    }
}