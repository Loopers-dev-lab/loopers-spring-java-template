package com.loopers.domain.like.event;

/**
 * Like 관련 이벤트
 */
public class LikeEvents {
    
    /**
     * 상품 좋아요 저장 완료 이벤트 (내부 이벤트)
     */
    public record ProductLikeSaved(Long productId) {
    }
    
    /**
     * 상품 좋아요 삭제 완료 이벤트 (내부 이벤트)
     */
    public record ProductLikeDeleted(Long productId) {
    }
    
    /**
     * 상품 좋아요 수 변경 이벤트
     * 다른 도메인(Product)에서 집계 처리를 위해 발행
     */
    public record LikeCountChanged(Long productId, long delta) {
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
    }
}

