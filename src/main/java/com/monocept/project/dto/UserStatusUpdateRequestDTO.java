package com.monocept.project.dto;

import com.monocept.project.util.TextNormalizationUtil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusUpdateRequestDTO {

	@NotNull(message = "Active status is required")
	private Boolean activeStatus;

	@NotBlank(message = "Remarks are required")
	@Size(min = 3, max = 255, message = "Remarks must be between 3 and 255 characters")
	private String remarks;

	public void setRemarks(String remarks) {
		this.remarks = TextNormalizationUtil.trimAndCollapseSpaces(remarks);
	}
}