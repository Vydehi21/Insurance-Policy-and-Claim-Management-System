package com.monocept.project.dto;

import jakarta.validation.constraints.Email;
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
    @Email(message = "Invalid email format")
    private String email;

    // Kept for backward compatibility with the existing frontend payload but
    // deliberately IGNORED by the service: the phone OTP is always checked
    // against the number saved on the pending registration. Trusting this
    // field let someone verify a different phone than the one registered.
    private String mobileNumber;

    @NotBlank(message = "Email OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Email OTP must contain 6 digits")
    private String emailOtp;

    @NotBlank(message = "Phone OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Phone OTP must contain 6 digits")
    private String phoneOtp;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}