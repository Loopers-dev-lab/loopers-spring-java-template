package com.loopers.application.like;

import com.loopers.application.like.event.LikeToggledEvent;
import com.loopers.domain.like.LikeService;
import com.loopers.infrastructure.cache.ProductCacheService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * Like 애플리케이션 파사드
 * - 트랜잭션 경계 관리
 * - 핵심 좋아요 로직 처리 (Like 엔티티 생성/삭제)
 * - 부가 로직은 이벤트로 분리 (좋아요 수 집계, 캐시 무효화)
 */
public class LikeFacade {

    private final LikeService likeService;
    private final ProductCacheService productCacheService;
    private final ApplicationEventPublisher eventPublisher;

    public LikeFacade(LikeService likeService, ProductCacheService productCacheService, ApplicationEventPublisher eventPublisher) {
        this.likeService = likeService;
        this.productCacheService = productCacheService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 좋아요 등록 (멱등)
     * - 좋아요 등록 후 이벤트 발행 (집계, 캐시 무효화는 이벤트 핸들러에서 처리)
     */
    @Transactional
    public void addLike(String userId, Long productId) {
        likeService.addLike(userId, productId);
        
        // 좋아요 추가 이벤트 발행 (좋아요 수 집계, 캐시 무효화 등)
        eventPublisher.publishEvent(LikeToggledEvent.added(userId, productId));
    }

    /**
     * 좋아요 취소 (멱등)
     * - 좋아요 취소 후 이벤트 발행 (집계, 캐시 무효화는 이벤트 핸들러에서 처리)
     */
    @Transactional
    public void removeLike(String userId, Long productId) {
        likeService.removeLike(userId, productId);
        
        // 좋아요 제거 이벤트 발행 (좋아요 수 집계, 캐시 무효화 등)
        eventPublisher.publishEvent(LikeToggledEvent.removed(userId, productId));
    }

    /**
     * 좋아요 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isLiked(String userId, Long productId) {
        return likeService.isLiked(userId, productId);
    }

    /**
     * 상품의 좋아요 수 조회
     */
    @Transactional(readOnly = true)
    public int getLikeCount(Long productId) {
        return likeService.getLikeCount(productId);
    }

    /**
     * 사용자가 좋아요한 상품 목록(IDs)
     */
    @Transactional(readOnly = true)
    public java.util.List<Long> getLikedProductIds(String userId) {
        return likeService.getLikedProductIds(userId);
    }
}
