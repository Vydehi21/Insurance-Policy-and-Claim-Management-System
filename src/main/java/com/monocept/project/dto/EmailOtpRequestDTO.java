package com.monocept.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtpRequestDTO {

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6 digit numeric code")
    private String otp;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}