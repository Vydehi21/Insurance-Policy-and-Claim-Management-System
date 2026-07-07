package com.monocept.project.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monocept.project.dto.EmailOtpRequestDTO;
import com.monocept.project.dto.PhoneOtpRequestDTO;
import com.monocept.project.exception.InvalidRequestException;
import com.monocept.project.service.OtpService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/otp")
public class OtpController {

	private final OtpService otpService;

	@PostMapping("/phone/send")
	public String sendPhone(@Valid @RequestBody PhoneOtpRequestDTO request) {
		requirePresent(request.getPhone(), "Phone number is required");
		return otpService.sendPhoneOtp(request.getPhone());
	}

	@PostMapping("/phone/verify")
	public boolean verifyPhone(@Valid @RequestBody PhoneOtpRequestDTO request) {
		requirePresent(request.getPhone(), "Phone number is required");
		requirePresent(request.getOtp(), "OTP is required");
		return otpService.verifyPhoneOtp(request.getPhone(), request.getOtp());
	}

	@PostMapping("/email/send")
	public String sendEmail(@Valid @RequestBody EmailOtpRequestDTO request) {
		requirePresent(request.getEmail(), "Email is required");
		otpService.sendEmailOtp(request.getEmail());
		return "Email OTP sent";
	}

	@PostMapping("/email/verify")
	public boolean verifyEmail(@Valid @RequestBody EmailOtpRequestDTO request) {
		requirePresent(request.getEmail(), "Email is required");
		requirePresent(request.getOtp(), "OTP is required");
		return otpService.verifyEmailOtp(request.getEmail(), request.getOtp());
	}

	private void requirePresent(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new InvalidRequestException(message);
		}
	}
}