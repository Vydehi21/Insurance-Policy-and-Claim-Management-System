package com.monocept.project.dto;

import com.monocept.project.enums.PaymentMode;
import com.monocept.project.enums.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPaymentDTO {

    private Long paymentId;

    @NotNull(message = "Policy ID association is required")
    private Long policyId;

    @NotNull(message = "Payment amount is required")
    @Min(value = 1, message = "Payment amount must be greater than zero")
    private Double amount;

    private LocalDateTime paymentDate;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionReference;

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}
