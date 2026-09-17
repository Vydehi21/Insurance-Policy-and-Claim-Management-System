package com.monocept.project.dto;

import com.monocept.project.enums.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long userId;
    private String fullName;
    private String email;
    private String mobileNumber;
    private Role role;
    private Boolean activeStatus;
    private LocalDateTime createdDate;

    private Long assignedProductId;
    private String assignedProductName;
}