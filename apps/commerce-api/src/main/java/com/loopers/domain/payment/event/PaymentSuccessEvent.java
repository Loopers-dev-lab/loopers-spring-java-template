package com.loopers.domain.payment.event;

import com.loopers.domain.payment.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 성공 이벤트
 * - 결제가 성공적으로 완료되었을 때 발행
 */
public record PaymentSuccessEvent(
    String eventId,
    String transactionKey,
    String orderId,
    String userId,
    BigDecimal amount,
    LocalDateTime completedAt
) {
    public static PaymentSuccessEvent from(Payment payment) {
        return new PaymentSuccessEvent(
            UUID.randomUUID().toString(),
            payment.getTransactionKey(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getAmount(),
            LocalDateTime.now()
        );
    }
}
