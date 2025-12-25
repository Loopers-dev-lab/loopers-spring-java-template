package com.loopers.domain.payment.event;

import com.loopers.domain.payment.CardType;

import java.time.LocalDateTime;

/**
 * 카드 결제 PG 처리 시작 이벤트
 *
 * Payment가 PENDING 상태로 저장된 후 발행되는 이벤트
 * PaymentEventListener가 이 이벤트를 수신하여 비동기로 PG를 호출
 *
 * startedAt: 이벤트 발생 시간 (PG 호출 시작 시간)
 */
public record CardPaymentProcessingStartedEvent(
        String paymentId,
        Long orderId,
        String userId,
        CardType cardType,
        String cardNo,
        LocalDateTime startedAt
) {
    public static CardPaymentProcessingStartedEvent of(
            String paymentId,
            Long orderId,
            String userId,
            CardType cardType,
            String cardNo
    ) {
        return new CardPaymentProcessingStartedEvent(
                paymentId,
                orderId,
                userId,
                cardType,
                cardNo,
                LocalDateTime.now()
        );
    }
}
