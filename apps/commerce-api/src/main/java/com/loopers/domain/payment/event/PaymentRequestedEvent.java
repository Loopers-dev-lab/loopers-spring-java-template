package com.loopers.domain.payment.event;

import com.loopers.domain.order.event.OrderCreatedEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 요청 이벤트
 * - 주문 생성 후 결제가 필요할 때 발행
 */
public record PaymentRequestedEvent(
    String eventId,
    Long orderId,
    String userId,
    BigDecimal amount,
    String cardType,      // TODO: 실제로는 주문에서 받아야 함
    String cardNo,        // TODO: 실제로는 주문에서 받아야 함
    LocalDateTime requestedAt
) {
    public static PaymentRequestedEvent from(OrderCreatedEvent orderEvent) {
        return new PaymentRequestedEvent(
            UUID.randomUUID().toString(),
            orderEvent.orderId(),
            orderEvent.userId(),
            orderEvent.finalAmount(),
            "SAMSUNG",  // TODO: 실제 카드 정보
            "1234-5678-9012-3456",  // TODO: 실제 카드 번호
            LocalDateTime.now()
        );
    }
}
