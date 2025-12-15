package com.loopers.domain.payment.event;

import com.loopers.domain.payment.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 실패 이벤트
 * - 결제가 실패했을 때 발행
 */
public record PaymentFailedEvent(
    String eventId,
    Long paymentId,
    String transactionKey,
    Long orderId,
    String userId,
    BigDecimal amount,
    String failureReason,
    LocalDateTime failedAt
) {
    public static PaymentFailedEvent from(Payment payment) {
        return new PaymentFailedEvent(
            UUID.randomUUID().toString(),
            payment.getId(),
            payment.getTransactionKey(),
            Long.parseLong(payment.getOrderId()),
            payment.getUserId(),
            payment.getAmount(),
            payment.getFailureReason(),
            LocalDateTime.now()
        );
    }
}
