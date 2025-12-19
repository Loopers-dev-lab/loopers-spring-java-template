package com.loopers.domain.stock.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loopers.shared.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockEvents {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderItemInfo(
        Long productId,
        Integer quantity
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Processed(
        Long orderId,
        List<OrderItemInfo> orderItems,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getPartitionKey() {
            return String.valueOf(orderId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProcessingFailed(
        Long orderId,
        List<OrderItemInfo> orderItems,
        String reason,
        LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getPartitionKey() {
            return String.valueOf(orderId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Compensated(
        Long orderId,
        LocalDateTime occurredAt
    ) implements DomainEvent {
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
