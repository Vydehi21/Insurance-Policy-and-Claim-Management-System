package com.monocept.project.dto;

import java.math.BigDecimal;

import com.monocept.project.enums.PaymentMode;
import com.monocept.project.enums.PaymentStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Digits(integer = 10, fraction = 0, message = "Amount must be a whole number (no decimals)")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotBlank(message = "Transaction reference is required")
    @Size(min = 4, max = 100, message = "Transaction reference must be between 4 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Transaction reference may only contain letters, digits, hyphens, and underscores")
    private String transactionReference;

    /**
     * Set by the (mock/simulated) payment gateway on the frontend only —
     * never trusted for anything financial. When true, the attempt is still
     * validated against every business rule below (policy state, amount,
     * duplicate reference, etc.) exactly as a real attempt would be, but the
     * outcome is persisted as a FAILED payment instead of SUCCESS, and the
     * policy's status/premium totals/due dates are left untouched. This lets
     * the gateway-declined path be tested end to end without a real payment
     * processor.
     */
    private boolean simulateFailure = false;

}