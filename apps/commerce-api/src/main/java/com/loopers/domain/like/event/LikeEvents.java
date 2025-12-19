package com.loopers.domain.like.event;

import com.loopers.shared.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Like 관련 이벤트
 */
public class LikeEvents {
    
    /**
     * 상품 좋아요 저장 완료 이벤트 (내부 이벤트)
     */
    public record ProductLikeSaved(String eventId, Long productId, LocalDateTime occurredAt) implements DomainEvent {
        public ProductLikeSaved(Long productId) {
            this(UUID.randomUUID().toString(), productId, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "LIKE";
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
    
    /**
     * 상품 좋아요 삭제 완료 이벤트 (내부 이벤트)
     */
    public record ProductLikeDeleted(String eventId, Long productId, LocalDateTime occurredAt) implements DomainEvent {
        public ProductLikeDeleted(Long productId) {
            this(UUID.randomUUID().toString(), productId, LocalDateTime.now());
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "LIKE";
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
    
    /**
     * 상품 좋아요 수 변경 이벤트
     * 다른 도메인(Product)에서 집계 처리를 위해 발행
     */
    public record LikeCountChanged(String eventId, Long productId, long delta, LocalDateTime occurredAt) implements DomainEvent {
        public LikeCountChanged(Long productId, long delta) {
            this(UUID.randomUUID().toString(), productId, delta, LocalDateTime.now());
        }
        
        /**
         * 좋아요 증가 (+1)
         */
        public static LikeCountChanged increment(Long productId) {
            return new LikeCountChanged(productId, 1L);
        }

        /**
         * 좋아요 감소 (-1)
         */
        public static LikeCountChanged decrement(Long productId) {
            return new LikeCountChanged(productId, -1L);
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getAggregateType() {
            return "LIKE";
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

