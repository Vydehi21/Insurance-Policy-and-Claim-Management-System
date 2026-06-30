package com.monocept.project.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDTO {


    @NotBlank(message="Full name is required")
    @Size(max=100,message="Name cannot exceed 100 characters")
    private String fullName;


    @Email(message="Invalid email format")
    @NotBlank(message="Email is required")
    private String email;


    @NotBlank(message="Mobile number is required")
    @Pattern(
        regexp="^[6-9][0-9]{9}$",
        message="Mobile number must be 10 digits"
    )
    private String mobileNumber;



    @NotNull(message="Date of birth is required")
    @Past(message="Date of birth must be past date")
    private LocalDate dateOfBirth;



    @NotBlank(message="Address is required")
    @Size(max=255)
    private String address;



    @NotBlank(message="City is required")
    private String city;



    @NotBlank(message="State is required")
    private String state;



    @NotBlank(message="PIN code required")
    @Pattern(
      regexp="^[0-9]{6}$",
      message="PIN code must be 6 digits"
    )
    private String pinCode;



    @NotBlank(message="Nominee name required")
    private String nomineeName;



    @NotBlank(message="Nominee relation required")
    private String nomineeRelation;

}