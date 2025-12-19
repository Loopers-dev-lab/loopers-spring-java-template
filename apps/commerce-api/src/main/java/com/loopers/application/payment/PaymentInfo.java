package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;

public record PaymentInfo(
        Long paymentId,
        String orderKey,
        Integer amount
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
                payment.getId(),
                payment.getOrderKey(),
                payment.getAmount()
        );
    }
}



