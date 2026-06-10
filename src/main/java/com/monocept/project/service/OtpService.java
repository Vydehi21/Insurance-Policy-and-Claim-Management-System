package com.monocept.project.service;

public interface OtpService {

	String sendPhoneOtp(String phone);

	boolean verifyPhoneOtp(String phone, String otp);

	void sendEmailOtp(String email);

	boolean verifyEmailOtp(String email, String otp);

}