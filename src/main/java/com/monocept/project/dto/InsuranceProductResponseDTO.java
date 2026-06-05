package com.monocept.project.dto;

import com.monocept.project.enums.ProductType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceProductResponseDTO {

    private Long productId;
    private String productName;
    private ProductType productType;
    private String description;
    private Boolean activeStatus;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
