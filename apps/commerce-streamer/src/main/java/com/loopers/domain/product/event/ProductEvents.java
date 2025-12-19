package com.loopers.domain.product.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loopers.domain.product.ProductStatus;
import com.loopers.shared.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEvents {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Created(
            Long productId,
            Long brandId,
            String name,
            BigDecimal price,
            ProductStatus status,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getPartitionKey() {
            return String.valueOf(productId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Updated(
            Long productId,
            Long brandId,
            String name,
            BigDecimal price,
            ProductStatus status,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getPartitionKey() {
            return String.valueOf(productId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Deleted(
            Long productId,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getPartitionKey() {
            return String.valueOf(productId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LikeCount(Long productId, long delta) implements DomainEvent {
        @Override
        public String getPartitionKey() {
            return String.valueOf(productId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return LocalDateTime.now();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Viewed(
            Long productId,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getPartitionKey() {
            return String.valueOf(productId);
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }
}
