package com.monocept.project.dto;

import com.monocept.project.util.PhoneNumberUtil;

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

    // Twilio Verify needs E.164 (+91...). The old pattern only accepted a bare
    // 10-digit number, which Twilio can't send to. Both shapes are accepted
    // now and normalized to +91XXXXXXXXXX.
    @Pattern(regexp = "^\\+91[6-9][0-9]{9}$", message = "Phone number must be a valid Indian mobile number")
    private String phone;

    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6 digit numeric code")
    private String otp;

    public void setPhone(String phone) {
        this.phone = PhoneNumberUtil.normalize(phone);
    }
}