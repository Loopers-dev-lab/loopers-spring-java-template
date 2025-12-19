package com.loopers.domain.product.event;

import com.loopers.domain.product.ProductStatus;
import com.loopers.shared.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product 관련 이벤트 DTO
 * 모든 Product 이벤트를 하나의 클래스로 통합 관리
 */
public class ProductEvents {

    /**
     * 상품 생성 이벤트
     */
    public record Created(
            Long productId,
            Long brandId,
            String name,
            BigDecimal price,
            ProductStatus status,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        public Created(Long productId, Long brandId, String name, BigDecimal price, ProductStatus status) {
            this(productId, brandId, name, price, status, LocalDateTime.now());
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
     * 상품 수정 이벤트
     */
    public record Updated(
            Long productId,
            Long brandId,
            String name,
            BigDecimal price,
            ProductStatus status,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        public Updated(Long productId, Long brandId, String name, BigDecimal price, ProductStatus status) {
            this(productId, brandId, name, price, status, LocalDateTime.now());
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
     * 상품 삭제 이벤트
     */
    public record Deleted(
            Long productId,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        public Deleted(Long productId) {
            this(productId, LocalDateTime.now());
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
     */
    public record LikeCount(Long productId, long delta) implements DomainEvent {
        /**
         * 좋아요 증가 (+1)
         */
        public static LikeCount increment(Long productId) {
            return new LikeCount(productId, 1L);
        }

        /**
         * 좋아요 감소 (-1)
         */
        public static LikeCount decrement(Long productId) {
            return new LikeCount(productId, -1L);
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

    /**
     * 상품 조회 이벤트
     */
    public record Viewed(
            Long productId,
            LocalDateTime occurredAt
    ) implements DomainEvent {
        public Viewed(Long productId) {
            this(productId, LocalDateTime.now());
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

