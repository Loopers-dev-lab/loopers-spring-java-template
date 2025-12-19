package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;

import java.time.ZonedDateTime;

public record PaymentInfo(
        Long id,
        String orderNo,
        String transactionKey,
        PaymentStatus status,
        CardType cardType,
        String cardNo,
        Long amount,
        Long pointUsed,
        String reason,
        boolean requiresRetry,
        ZonedDateTime createdAt
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
                payment.getId(),
                payment.getOrder().getOrderNo(),
                payment.getTransactionKey(),
                payment.getStatus(),
                payment.getCardType(),
                payment.getCardNo(),
                payment.getAmount(),
                payment.getPointUsed(),
                payment.getReason(),
                payment.isRequiresRetry(),
                payment.getCreatedAt()
        );
    }
}
