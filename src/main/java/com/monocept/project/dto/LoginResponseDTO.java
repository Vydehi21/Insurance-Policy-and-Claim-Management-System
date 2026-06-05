package com.monocept.project.dto;

import com.monocept.project.enums.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String jwtToken;
    private String tokenType = "Bearer";
    private String userEmail;
    private Role userRole;
    private Long tokenExpiryInformation;
}
