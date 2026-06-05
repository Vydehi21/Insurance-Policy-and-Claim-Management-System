package com.monocept.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDocumentDTO {

    @NotBlank(message = "Document name is required")
    @Size(max = 100)
    private String documentName;

    @NotBlank(message = "Document type is required")
    @Size(max = 50)
    private String documentType;

    @NotBlank(message = "Document reference is required")
    @Size(max = 255)
    private String documentReference;
}
