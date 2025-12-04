package com.loopers.interfaces.api.payment;

import com.loopers.domain.payment.Payment;
import java.math.BigDecimal;

public record PaymentResponse(
    String transactionKey,
    String orderId,
    BigDecimal amount,
    String status,
    String failureReason
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getTransactionKey(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getStatus().name(),
            payment.getFailureReason()
        );
    }
}
