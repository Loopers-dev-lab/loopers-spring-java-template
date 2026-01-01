package com.loopers.application.event;

import com.loopers.application.payment.TransactionStatus;
import com.loopers.domain.order.Money;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDataTransferEvent(
    String orderId,
    Long userId,
    Money amount,
    CardType cardType,
    PaymentStatus paymentStatus,
    TransactionStatus transactionStatus,
    String reason,
    LocalDateTime processedAt,
    String eventType // "PAYMENT_REQUESTED", "PAYMENT_SUCCESS", "PAYMENT_FAILED"
) {
}
