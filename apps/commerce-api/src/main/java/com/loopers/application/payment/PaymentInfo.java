package com.loopers.application.payment;

import java.math.BigDecimal;

import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentStatus;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentInfo(
        Long id,
        String transactionKey,
        Long orderId,
        BigDecimal amount,
        PaymentStatus status,
        String reason) {
    public static PaymentInfo from(PaymentEntity entity) {
        return new PaymentInfo(
                entity.getId(),
                entity.getTransactionKey(),
                entity.getOrderNumber(),
                entity.getAmount(),
                entity.getPaymentStatus(),
                entity.getFailureReason()
        );
    }

    public static PaymentInfo pending(PaymentEntity entity) {
        return new PaymentInfo(
                entity.getId(),
                entity.getTransactionKey(),
                entity.getOrderNumber(),
                entity.getAmount(),
                PaymentStatus.PENDING,
                null
        );
    }
}
