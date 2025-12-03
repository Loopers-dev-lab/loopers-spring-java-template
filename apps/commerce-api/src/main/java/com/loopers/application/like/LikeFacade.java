package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Like 애플리케이션 파사드
 * - Application Layer에서 트랜잭션/검증/로깅/권한과 같은 관심사를 다루고,
 *   도메인 규칙 실행은 {@link LikeService}에 위임한다.
 */
public class LikeFacade {

    private final LikeService likeService;

    public LikeFacade(LikeService likeService) {
        this.likeService = likeService;
    }

    /**
     * 좋아요 등록 (멱등)
     */
    @Transactional
    public void addLike(String userId, Long productId) {
        likeService.addLike(userId, productId);
    }

    /**
     * 좋아요 취소 (멱등)
     */
    @Transactional
    public void removeLike(String userId, Long productId) {
        likeService.removeLike(userId, productId);
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
