package com.monocept.project.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOtpRequestDTO {

	@Pattern(regexp = "^[6-9][0-9]{9}$", message = "Phone number must be a valid 10 digit Indian number")
    private String phone;

    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6 digit numeric code")
    private String otp;

}