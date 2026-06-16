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
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true)
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
    @Pattern(
    	    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
    	    message = "Password must contain uppercase, lowercase and number"
    	)
    private String password;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 15, message = "Mobile number must not exceed 15 characters")
    @Pattern(
            regexp = "^\\+[1-9]\\d{7,14}$",
            message = "Mobile number must be in international format like +919876543210"
    )
    @Column(unique = true)
    private String mobileNumber;
    
    
}
