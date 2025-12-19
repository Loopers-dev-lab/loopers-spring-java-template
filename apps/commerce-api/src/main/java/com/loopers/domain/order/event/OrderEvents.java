package com.loopers.domain.order.event;

import com.loopers.domain.payment.PaymentDto;
import com.loopers.shared.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order 관련 이벤트 DTO
 */
public class OrderEvents {

    /**
     * 주문 생성 이벤트
     */
    public record Created(
            String eventId,
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            List<OrderItemInfo> items,
            List<Long> couponIds,
            PaymentDto.PaymentMethod paymentMethod,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        public Created(Long orderId, Long userId, BigDecimal totalAmount, List<OrderItemInfo> items, List<Long> couponIds, PaymentDto.PaymentMethod paymentMethod) {
            this(UUID.randomUUID().toString(), orderId, userId, totalAmount, items, couponIds, paymentMethod, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "ORDER";
        }
        
        @Override
        public String getPartitionKey() {
            return String.valueOf(orderId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }

    /**
     * 주문 항목 정보 (Event 내장용 DTO)
     */
    public record OrderItemInfo(
            Long productId,
            String productName,
            BigDecimal price,
            Integer quantity
    ) {
    }

    /**
     * 주문 확인(완료) 이벤트
     */
    public record Confirmed(
            String eventId,
            Long orderId,
            Long userId,
            String orderStatus,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        public Confirmed(Long orderId, Long userId, String orderStatus) {
            this(UUID.randomUUID().toString(), orderId, userId, orderStatus, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "ORDER";
        }
        
        @Override
        public String getPartitionKey() {
            return String.valueOf(orderId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }
}