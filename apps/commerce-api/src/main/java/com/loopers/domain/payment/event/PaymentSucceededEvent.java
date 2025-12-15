package com.loopers.domain.payment.event;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;

import java.time.ZonedDateTime;

public record PaymentSucceededEvent(
        Long paymentId,
        Long orderId,
        Long userId,
        Long couponId,
        Long amount,
        PaymentMethod paymentMethod,
        ZonedDateTime paidAt
) {
    public static PaymentSucceededEvent of(Payment payment, Long couponId, ZonedDateTime paidAt) {
        return new PaymentSucceededEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                couponId,
                payment.getAmountValue(),
                payment.getPaymentMethod(),
                paidAt != null ? paidAt : ZonedDateTime.now()
        );
    }
}
