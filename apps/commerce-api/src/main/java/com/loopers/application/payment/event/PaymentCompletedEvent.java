package com.loopers.application.payment.event;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트
 * - 결제가 성공적으로 완료된 후 발행되는 이벤트
 * - 주문 완료 처리, 데이터 플랫폼 전송 등 후속 처리를 트리거한다
 */
public record PaymentCompletedEvent(
        Long paymentId,
        String userId,
        Long orderId,
        BigDecimal amount,
        PaymentStatus status,
        String pgTransactionKey,
        LocalDateTime completedAt
) {
    public static PaymentCompletedEvent from(Payment payment) {
        return new PaymentCompletedEvent(
                payment.getId(),
                payment.getUserId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPgTransactionKey(),
                LocalDateTime.now()
        );
    }

    /**
     * 결제가 성공했는지 확인
     */
    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }
}
