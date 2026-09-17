package com.monocept.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequestDto {

	@Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Phone number must be a valid 10 digit Indian number")
    private String phone;
    
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6 digit numeric code")
    private String otp;
}

