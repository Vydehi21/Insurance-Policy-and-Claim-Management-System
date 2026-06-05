package com.monocept.project.dto;

import com.monocept.project.enums.*;
import jakarta.validation.constraints.NotBlank;
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
public class InsuranceProductDTO {

    private Long productId;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    private String productName;

    @NotNull(message = "Product type is required")
    private ProductType productType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean activeStatus;
}
