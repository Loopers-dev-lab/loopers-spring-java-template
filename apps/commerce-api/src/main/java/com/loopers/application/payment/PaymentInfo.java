package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentStatus;

import java.io.Serializable;
import java.time.ZonedDateTime;

public record PaymentInfo(
        Long paymentId,
        Long orderId,
        Long userId,
        Long amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String transactionId,
        String cardType,
        String cardNo,
        String failureReason,
        String idempotencyKey,
        ZonedDateTime createdAt
) implements Serializable {

    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmountValue(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getCardType(),
                payment.getCardNo(),
                payment.getFailureReason(),
                payment.getIdempotencyKey(),
                payment.getCreatedAt()
        );
    }
}
