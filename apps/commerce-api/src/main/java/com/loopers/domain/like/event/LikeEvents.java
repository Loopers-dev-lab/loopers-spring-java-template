package com.loopers.domain.like.event;

/**
 * Like 관련 내부 이벤트
 * 좋아요 저장/삭제 후 집계 이벤트 발행을 위한 내부 이벤트
 */
public class LikeEvents {
    
    /**
     * 상품 좋아요 저장 완료 이벤트
     */
    public record ProductLikeSaved(Long productId) {
    }
    
    /**
     * 상품 좋아요 삭제 완료 이벤트
     */
    public record ProductLikeDeleted(Long productId) {
    }
}

