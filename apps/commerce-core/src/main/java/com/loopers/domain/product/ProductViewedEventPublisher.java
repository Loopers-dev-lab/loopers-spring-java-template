package com.loopers.domain.product;

/**
 * 상품 조회 이벤트를 발행하는 Publisher 인터페이스
 */
public interface ProductViewedEventPublisher {
    /**
     * 상품 조회 이벤트를 발행합니다.
     * 
     * @param productId 상품 ID
     */
    void publishProductViewed(Long productId);
}

