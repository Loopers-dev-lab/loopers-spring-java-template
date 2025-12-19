package com.loopers.domain.stock.event;

import com.loopers.domain.order.event.OrderEvents;
import com.loopers.shared.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class StockEvents {
    
    public record OrderItemInfo(
        Long productId,
        Integer quantity
    ) {}
    
    public record Processed(
        String eventId,
        Long orderId,
        List<OrderItemInfo> orderItems,  // 재고 원복을 위해 필요
        OrderEvents.Created originalEvent,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public Processed(Long orderId, List<OrderItemInfo> orderItems, OrderEvents.Created originalEvent) {
            this(UUID.randomUUID().toString(), orderId, orderItems, originalEvent, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "STOCK";
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
        String eventId,
        Long orderId,
        List<OrderItemInfo> orderItems,  // 재고 원복을 위해 필요
        String reason,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public ProcessingFailed(Long orderId, List<OrderItemInfo> orderItems, String reason) {
            this(UUID.randomUUID().toString(), orderId, orderItems, reason, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "STOCK";
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
        String eventId,
        Long orderId,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        public Compensated(Long orderId) {
            this(UUID.randomUUID().toString(), orderId, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "STOCK";
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

