package com.monocept.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Emails are stored trimmed + lowercase at registration, so normalize the
    // login input the same way (leading/trailing spaces, mixed case).
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}