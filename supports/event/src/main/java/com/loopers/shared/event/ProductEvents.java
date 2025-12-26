package com.loopers.shared.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product 관련 이벤트
 * 공통 모듈로 이동하여 타입 안전성 확보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEvents {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Created(
            String eventId,
            Long productId,
            Long brandId,
            String name,
            BigDecimal price,
            String status, // ProductStatus enum 대신 String 사용
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PRODUCT";
        }
        
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
            String eventId,
            Long productId,
            Long brandId,
            String name,
            BigDecimal price,
            String status, // ProductStatus enum 대신 String 사용
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PRODUCT";
        }
        
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
            String eventId,
            Long productId,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PRODUCT";
        }
        
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
    public record LikeCount(String eventId, Long productId, long delta) implements DomainEvent {
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PRODUCT";
        }
        
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
            String eventId,
            Long productId,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "PRODUCT";
        }
        
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

