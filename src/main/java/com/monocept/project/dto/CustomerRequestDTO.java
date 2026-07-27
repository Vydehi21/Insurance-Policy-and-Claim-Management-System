package com.monocept.project.dto;

import java.time.LocalDate;

import com.monocept.project.util.TextNormalizationUtil;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDTO {

 //   @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Name should contain only alphabets")
    private String fullName;

    @Email(message = "Invalid email format")
 //   @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

 //   @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Mobile number must be a valid 10 digit Indian number")
    private String mobileNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "City should contain only alphabets")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "State should contain only alphabets")
    private String state;

    @NotBlank(message = "PIN code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be a valid 6 digit Indian PIN (cannot start with 0)")
    private String pinCode;

    @NotBlank(message = "Nominee name is required")
    @Size(min = 3, max = 100, message = "Nominee name must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Nominee name should contain only alphabets")
    private String nomineeName;

    @NotBlank(message = "Nominee relation is required")
    @Size(max = 50, message = "Nominee relation must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Nominee relation should contain only alphabets (e.g. Spouse, Father, Mother, Son, Daughter)")
    private String nomineeRelation;

    public void setFullName(String fullName) {
        this.fullName = TextNormalizationUtil.toTitleCase(fullName);
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }

    public void setAddress(String address) {
        this.address = TextNormalizationUtil.trimAndCollapseSpaces(address);
    }

    public void setCity(String city) {
        this.city = TextNormalizationUtil.toTitleCase(city);
    }

    public void setState(String state) {
        this.state = TextNormalizationUtil.toTitleCase(state);
    }

    public void setNomineeName(String nomineeName) {
        this.nomineeName = TextNormalizationUtil.toTitleCase(nomineeName);
    }

    public void setNomineeRelation(String nomineeRelation) {
        this.nomineeRelation = TextNormalizationUtil.toTitleCase(nomineeRelation);
    }
}