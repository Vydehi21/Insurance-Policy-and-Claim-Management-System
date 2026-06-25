package com.monocept.project.controller;

import java.util.Map;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monocept.project.dto.ForgotPasswordRequestDTO;
import com.monocept.project.dto.LoginRequestDTO;
import com.monocept.project.dto.LoginResponseDTO;
import com.monocept.project.dto.RegistrationRequestDTO;
import com.monocept.project.dto.ResendRegistrationOtpDTO;
import com.monocept.project.dto.ResetPasswordRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.VerifyRegistrationOtpDTO;
import com.monocept.project.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
@Tag(name = "auth", description = "Authentication and registration management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register Customer", description = "Registers a new customer user into the system")
    public ResponseEntity<String> registerCustomer(
            @Valid @RequestBody RegistrationRequestDTO request) {

        return ResponseEntity.ok(
                authService.registerCustomer(request));
    }
    
    @PostMapping("/verify-register")
    public ResponseEntity<UserResponseDTO> verifyRegister(
    		@Valid @RequestBody VerifyRegistrationOtpDTO request){
    	
    	return ResponseEntity
    			.status(HttpStatus.CREATED)
    			.body(authService.verifyRegister(request));
    }
    
    @PostMapping("/register/resend-otp")
    public ResponseEntity<String> resendRegistrationOtp(
            @RequestBody ResendRegistrationOtpDTO dto){

        return ResponseEntity.ok(
                authService.resendRegistrationOtp(dto)
        );
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates user credentials and returns a secure JWT access token")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<String>
    forgotPassword(
            @RequestBody
            ForgotPasswordRequestDTO request) {

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                "Password reset email sent");
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<String>
    resetPassword(
            @RequestBody
            ResetPasswordRequestDTO request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                "Password reset successful");
    }
}
