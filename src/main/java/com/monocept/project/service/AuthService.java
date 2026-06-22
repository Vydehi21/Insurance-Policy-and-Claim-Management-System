package com.monocept.project.service;

import com.monocept.project.dto.ForgotPasswordRequestDTO;
import com.monocept.project.dto.LoginRequestDTO;
import com.monocept.project.dto.LoginResponseDTO;
import com.monocept.project.dto.RegistrationRequestDTO;
import com.monocept.project.dto.ResendRegistrationOtpDTO;
import com.monocept.project.dto.ResetPasswordRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.VerifyRegistrationOtpDTO;

public interface AuthService {
    String registerCustomer(RegistrationRequestDTO registrationDTO);
    UserResponseDTO verifyRegister(VerifyRegistrationOtpDTO verifyDTO);
    String resendRegistrationOtp(ResendRegistrationOtpDTO resendRegistrationOtpDTO);
    LoginResponseDTO login(LoginRequestDTO loginDTO);
    void   forgotPassword(
            ForgotPasswordRequestDTO request);

    void resetPassword(
            ResetPasswordRequestDTO request);
    
}
