package com.monocept.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequestDto {

    private String email;
    private String phone;
    private String otp;
}