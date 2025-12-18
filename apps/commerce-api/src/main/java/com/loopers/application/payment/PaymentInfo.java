package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;

public record PaymentInfo(
        PaymentStatus paymentStatus,
        CardType cardType,
        String cardNo,
        Long amount,
        String failReason,
        Long orderId,
        String transactionKey
) {
    public static PaymentInfo from(final Payment payment) {
        return new PaymentInfo(
                payment.getPaymentStatus(),
                payment.getCardType(),
                payment.getCardNo(),
                payment.getAmount(),
                payment.getFailReason(),
                payment.getOrderId(),
                payment.getTransactionKey()
        );
    }
}
