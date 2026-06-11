package com.monocept.project.service;

import com.monocept.project.dto.ForgotPasswordRequestDTO;
import com.monocept.project.dto.LoginRequestDTO;
import com.monocept.project.dto.LoginResponseDTO;
import com.monocept.project.dto.RegistrationRequestDTO;
import com.monocept.project.dto.ResetPasswordRequestDTO;
import com.monocept.project.dto.UserResponseDTO;

public interface AuthService {
    UserResponseDTO registerCustomer(RegistrationRequestDTO registrationDTO);
    LoginResponseDTO login(LoginRequestDTO loginDTO);
    void   forgotPassword(
            ForgotPasswordRequestDTO request);

    void resetPassword(
            ResetPasswordRequestDTO request);
    
}
