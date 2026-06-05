package com.monocept.project.dto;

import com.monocept.project.enums.PaymentMode;
import com.monocept.project.enums.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPaymentResponseDTO {

    private Long paymentId;
    private String policyNumber;
    private Double amount;
    private LocalDateTime paymentDate;
    private PaymentMode paymentMode;
    private String transactionReference;
    private PaymentStatus paymentStatus;
}
