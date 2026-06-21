package com.monocept.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRegistrationOtpDTO {

    @NotBlank(message = "Email is required")
    private String email;


    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;


    @NotBlank(message = "Email OTP is required")
    @Pattern(
        regexp = "^[0-9]{6}$",
        message = "Email OTP must contain 6 digits"
    )
    private String emailOtp;


    @NotBlank(message = "Phone OTP is required")
    @Pattern(
        regexp = "^[0-9]{6}$",
        message = "Phone OTP must contain 6 digits"
    )
    private String phoneOtp;
}