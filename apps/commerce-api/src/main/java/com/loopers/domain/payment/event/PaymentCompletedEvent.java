package com.loopers.domain.payment.event;

import com.loopers.domain.payment.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트 (Domain Event)
 *
 * 결제가 성공적으로 완료되었음을 알리는 진짜 Domain Event입니다.
 * 다른 서비스나 시스템에서 이 이벤트를 구독하여 부가적인 처리를 할 수 있습니다.
 *
 * 예시:
 * - 알림 서비스: 결제 완료 알림 발송
 * - 분석 서비스: 결제 통계 수집
 * - 배송 서비스: 배송 프로세스 시작
 */
public record PaymentCompletedEvent(
        String paymentId,
        Long orderId,
        Long userId,
        PaymentType paymentType,
        BigDecimal amount,
        LocalDateTime completedAt
) {
    public static PaymentCompletedEvent of(
            String paymentId,
            Long orderId,
            Long userId,
            PaymentType paymentType,
            BigDecimal amount
    ) {
        return new PaymentCompletedEvent(
                paymentId,
                orderId,
                userId,
                paymentType,
                amount,
                LocalDateTime.now()
        );
    }
}