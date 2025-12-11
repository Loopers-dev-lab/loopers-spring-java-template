package com.loopers.domain.product.event;

/**
 * Product 관련 이벤트 DTO
 * 모든 Product 이벤트를 하나의 클래스로 통합 관리
 */
public class ProductEvents {

    /**
     * 상품 생성 이벤트
     */
    public record Created(Long productId, Long brandId) {
    }

    /**
     * 상품 수정 이벤트
     */
    public record Updated(Long productId, Long brandId) {
    }

    /**
     * 상품 삭제 이벤트
     */
    public record Deleted(Long productId) {
    }

    /**
     * 상품 좋아요 수 변경 이벤트
     */
    public record LikeCount(Long productId, long delta) {
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
    }
}

