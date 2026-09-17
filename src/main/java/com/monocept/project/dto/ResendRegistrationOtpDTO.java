package com.monocept.project.dto;

import jakarta.validation.constraints.Email;
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
    @Email(message = "Invalid email format")
    private String email;

    // Optional and ignored: OTPs are always re-sent to the number stored on
    // the pending registration.
    private String mobileNumber;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}