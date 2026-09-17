package com.monocept.project.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "email_otps",
    indexes = {
        @Index(name = "idx_email_otp_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtp {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String email;

	private String otp;

	private LocalDateTime expiryTime;

	private boolean verified;

	// Wrong guesses against this OTP. After MAX_ATTEMPTS the OTP is deleted and
	// a new one must be requested (brute-force protection).
	@Column(nullable = false, columnDefinition = "int default 0")
	private int attempts = 0;

	// When this OTP was issued; used for the resend cooldown.
	private LocalDateTime createdAt;

}