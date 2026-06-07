package com.monocept.project.dto;

import java.math.BigDecimal;

import com.monocept.project.enums.PaymentMode;
import com.monocept.project.enums.PaymentStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPaymentRequestDTO {

    @NotNull(message = "Policy reference is required")
    private Long policyId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotNull(message = "Transaction reference is required")
    @Size(max = 100)
    private String transactionReference;

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}
