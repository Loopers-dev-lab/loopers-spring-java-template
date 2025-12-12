package com.loopers.infrastructure.platform;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentStatus;

import java.time.ZonedDateTime;

public record PaymentResultMessage(
        Long paymentId,
        Long orderId,
        Long userId,
        PaymentAction action,
        Long amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String failureReason,
        ZonedDateTime occurredAt
) {
    public enum PaymentAction {
        SUCCESS,
        FAILED
    }

    public static PaymentResultMessage success(
            Long paymentId, Long orderId, Long userId,
            Long amount, PaymentMethod paymentMethod
    ) {
        return new PaymentResultMessage(
                paymentId, orderId, userId,
                PaymentAction.SUCCESS, amount, paymentMethod, PaymentStatus.SUCCESS, null,
                ZonedDateTime.now()
        );
    }

    public static PaymentResultMessage failed(
            Long paymentId, Long orderId, Long userId,
            Long amount, PaymentMethod paymentMethod, PaymentStatus status, String reason
    ) {
        return new PaymentResultMessage(
                paymentId, orderId, userId,
                PaymentAction.FAILED, amount, paymentMethod, status, reason,
                ZonedDateTime.now()
        );
    }
}
