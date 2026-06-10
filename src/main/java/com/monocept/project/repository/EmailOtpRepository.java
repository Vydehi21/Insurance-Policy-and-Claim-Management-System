package com.monocept.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monocept.project.model.EmailOtp;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

	Optional<EmailOtp> findByEmail(String email);

}