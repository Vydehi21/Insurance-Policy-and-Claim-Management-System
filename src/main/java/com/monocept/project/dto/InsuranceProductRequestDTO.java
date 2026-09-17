package com.monocept.project.dto;

import com.monocept.project.enums.ProductType;
import com.monocept.project.util.TextNormalizationUtil;

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
public class InsuranceProductRequestDTO {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "Product name can only contain letters, numbers and spaces")
    private String productName;

    @NotNull(message = "Product type is required")
    private ProductType productType;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    @Pattern(regexp = ".*[A-Za-z]{3,}.*", message = "Description must contain meaningful text")
    private String description;

    @NotNull(message = "Active status is required")
    private Boolean activeStatus;

    public void setProductName(String productName) {
        this.productName = TextNormalizationUtil.toTitleCase(productName);
    }

    public void setDescription(String description) {
        this.description = TextNormalizationUtil.trimAndCollapseSpaces(description);
    }
}