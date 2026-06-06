package com.monocept.project.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponseDTO {

    private LocalDateTime timestamp;

    private Integer statusCode;

    private String errorType;

    private Map<String, String> validationErrors;

    private String path;
}