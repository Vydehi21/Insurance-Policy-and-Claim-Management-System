package com.monocept.project.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[A-Za-z ]+$",
            message = "Name should contain only alphabets")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true)
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    
    

    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+91[0-9]{10}$",
            message = "Invalid mobile number")
    private String mobileNumber;
    
    public void setFullName(String fullName) {
        this.fullName = fullName.trim();
    }

}
