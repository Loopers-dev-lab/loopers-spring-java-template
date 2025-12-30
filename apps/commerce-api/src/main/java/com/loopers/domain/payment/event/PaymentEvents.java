package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.shared.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentEvents {
    
    /**
     * PG 콜백 수신 이벤트
     * PaymentFacade에서 PG 콜백을 받아 발행
     */
    public record CallbackReceived(
        String eventId,
        String transactionKey,
        Long orderId,
        PaymentDto.PaymentStatus status,
        String reason,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public CallbackReceived(String transactionKey, Long orderId, PaymentDto.PaymentStatus status, String reason) {
            this(UUID.randomUUID().toString(), transactionKey, orderId, status, reason, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PAYMENT";
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
     * 결제 처리 완료 이벤트
     */
    public record Processed(
        String eventId,
        Long orderId,
        Long userId,  // 결제 처리를 위해 필요
        BigDecimal finalAmount,  // 최종 결제 금액
        CouponEvents.Processed originalEvent,  // nullable: PG 콜백 경로에서는 null
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public Processed(Long orderId, Long userId, BigDecimal finalAmount, CouponEvents.Processed originalEvent) {
            this(UUID.randomUUID().toString(), orderId, userId, finalAmount, originalEvent, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PAYMENT";
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
     * 결제 처리 실패 이벤트
     */
    public record ProcessingFailed(
        String eventId,
        Long orderId,
        CouponEvents.Processed originalEvent,  // 재고 원복을 위해 필요 (nullable)
        String reason,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public ProcessingFailed(Long orderId, CouponEvents.Processed originalEvent, String reason) {
            this(UUID.randomUUID().toString(), orderId, originalEvent, reason, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PAYMENT";
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

