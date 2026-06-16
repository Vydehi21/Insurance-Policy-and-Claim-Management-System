package com.monocept.project.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monocept.project.dto.OtpRequestDto;
import com.monocept.project.service.OtpService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

	private final OtpService otpService;

	public OtpController(OtpService otpService) {

		this.otpService = otpService;

	}

	@PostMapping("/phone/send")
	public String sendPhone(@Valid @RequestBody OtpRequestDto request) {

	    return otpService.sendPhoneOtp(
	            request.getPhone()
	    );
	}

	@PostMapping("/phone/verify")
	public boolean verifyPhone(@RequestBody OtpRequestDto request) {

	    return otpService.verifyPhoneOtp(
	            request.getPhone(),
	            request.getOtp()
	    );
	}

	@PostMapping("/email/send")
	public String sendEmail(@Valid @RequestBody OtpRequestDto request) {

	    otpService.sendEmailOtp(request.getEmail());

	    return "Email OTP sent";
	}

	@PostMapping("/email/verify")
	public boolean verifyEmail(@Valid @RequestBody OtpRequestDto request) {

	    return otpService.verifyEmailOtp(
	            request.getEmail(),
	            request.getOtp()
	    );
	}

}