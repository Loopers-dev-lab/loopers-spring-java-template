package com.loopers.domain.coupon.event;

import com.loopers.domain.stock.event.StockEvents;
import com.loopers.shared.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponEvents {
    
    public record Processed(
        Long orderId,
        Long userId,
        BigDecimal totalDiscountAmount,  // 총 할인 금액
        StockEvents.Processed originalEvent,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public Processed(Long orderId, Long userId, BigDecimal totalDiscountAmount, StockEvents.Processed originalEvent) {
            this(orderId, userId, totalDiscountAmount, originalEvent, LocalDateTime.now());
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
    
    public record ProcessingFailed(
        Long orderId,
        StockEvents.Processed originalEvent,  // 재고 원복을 위해 필요
        String reason,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public ProcessingFailed(Long orderId, StockEvents.Processed originalEvent, String reason) {
            this(orderId, originalEvent, reason, LocalDateTime.now());
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
    
    public record Compensated(
        Long orderId,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public Compensated(Long orderId) {
            this(orderId, LocalDateTime.now());
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

