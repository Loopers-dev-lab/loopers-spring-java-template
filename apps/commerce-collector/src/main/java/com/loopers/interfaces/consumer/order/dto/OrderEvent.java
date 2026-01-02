package com.loopers.interfaces.consumer.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka 주문 이벤트 Envelope
 * Outbox 패턴의 메시지 구조를 표현
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEvent(
        String eventId,
        String eventType,
        OrderCreatedPayload payload
) {
    /**
     * ORDER_CREATED 이벤트의 payload 구조
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderCreatedPayload(
            Long orderId,
            Long userId,
            BigDecimal totalPrice,
            String paymentType,  // POINT or CARD
            List<OrderItem> items,
            LocalDateTime createdAt
    ) {
        public record OrderItem(
                Long productId,
                Integer quantity,
                BigDecimal price
        ) {}
    }
}
