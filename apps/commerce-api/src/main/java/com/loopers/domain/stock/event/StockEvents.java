package com.loopers.domain.stock.event;

import com.loopers.domain.order.event.OrderEvents;
import com.loopers.shared.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.List;

public class StockEvents {
    
    public record OrderItemInfo(
        Long productId,
        Integer quantity
    ) {}
    
    public record Processed(
        Long orderId,
        List<OrderItemInfo> orderItems,  // 재고 원복을 위해 필요
        OrderEvents.Created originalEvent,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public Processed(Long orderId, List<OrderItemInfo> orderItems, OrderEvents.Created originalEvent) {
            this(orderId, orderItems, originalEvent, LocalDateTime.now());
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
        List<OrderItemInfo> orderItems,  // 재고 원복을 위해 필요
        String reason,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public ProcessingFailed(Long orderId, List<OrderItemInfo> orderItems, String reason) {
            this(orderId, orderItems, reason, LocalDateTime.now());
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

