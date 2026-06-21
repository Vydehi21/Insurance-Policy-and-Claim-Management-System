package com.monocept.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResendRegistrationOtpDTO {

    @NotBlank(message = "Email is required")
    private String email;


    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;
}