package com.monocept.project.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.monocept.project.enums.PaymentMode;
import com.monocept.project.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "premium_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPayment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "policy_id", nullable = false)
	private Policy policy;
	
	@Column(nullable = false)
	private BigDecimal amount;
	
	@Column(nullable = false)
	private LocalDateTime paymentDate;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentMode paymentMode;
	
	@Column(nullable = false, unique = true)
	private String transactionReference;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus paymentStatus;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdDate;
	
	@PrePersist
    public void beforeSave() {
        createdDate = LocalDateTime.now();
        if(paymentDate == null) paymentDate = LocalDateTime.now();
    }
}
