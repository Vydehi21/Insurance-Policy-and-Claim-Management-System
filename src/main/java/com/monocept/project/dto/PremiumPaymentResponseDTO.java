package com.monocept.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.monocept.project.enums.PaymentMode;
import com.monocept.project.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPaymentResponseDTO {

    private Long paymentId;
    private String policyNumber;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private PaymentMode paymentMode;
    private String transactionReference;
    private PaymentStatus paymentStatus;
    private LocalDate nextPremiumDueDate;
    private String customerName;
}
