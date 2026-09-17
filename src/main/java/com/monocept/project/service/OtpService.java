package com.monocept.project.service;

public interface OtpService {

	String sendPhoneOtp(String phone);

	boolean verifyPhoneOtp(String phone, String otp);

	void sendEmailOtp(String email);

	boolean verifyEmailOtp(String email, String otp);

	/**
	 * Throws if an email OTP was issued to this address too recently.
	 * Called before any OTP (email or SMS) is sent so a resend can't be spammed.
	 */
	void assertResendAllowed(String email);
}