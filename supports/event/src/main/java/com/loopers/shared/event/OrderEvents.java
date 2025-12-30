package com.loopers.shared.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order 관련 이벤트 DTO
 * 공통 모듈로 이동하여 타입 안전성 확보
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
            String paymentMethod, // PaymentDto.PaymentMethod 대신 String 사용 (JSON 역직렬화 호환)
            LocalDateTime occurredAt
    ) implements DomainEvent {
        public Created(Long orderId, Long userId, BigDecimal totalAmount, List<OrderItemInfo> items, List<Long> couponIds, String paymentMethod) {
            this(UUID.randomUUID().toString(), orderId, userId, totalAmount, items, couponIds, paymentMethod, LocalDateTime.now());
        }
        
        /**
         * PaymentMethod enum을 받는 생성자 (기존 코드 호환성)
         * JSON 역직렬화 시 enum이 String으로 변환되어 이 생성자가 호출됨
         */
        public Created(Long orderId, Long userId, BigDecimal totalAmount, List<OrderItemInfo> items, List<Long> couponIds, Object paymentMethod) {
            this(UUID.randomUUID().toString(), orderId, userId, totalAmount, items, couponIds, 
                paymentMethod != null ? paymentMethod.toString() : null, LocalDateTime.now());
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

