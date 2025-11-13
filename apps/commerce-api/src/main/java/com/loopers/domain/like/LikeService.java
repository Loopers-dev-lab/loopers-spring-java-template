package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 좋아요 도메인 서비스
 * - 좋아요 등록/취소 비즈니스 로직
 * - 멱등성 보장
 */
@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;

    /**
     * 좋아요 등록
     * - 멱등성 보장: 이미 좋아요한 경우 무시
     */
    @Transactional
    public void addLike(String userId, Long productId) {
        // 이미 존재하면 무시 (멱등성)
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        Like like = Like.create(userId, productId);
        likeRepository.save(like);
    }

    /**
     * 좋아요 취소
     * - 멱등성 보장: 이미 취소된 경우에도 에러 없음
     */
    @Transactional
    public void removeLike(String userId, Long productId) {
        // 존재 여부 확인 없이 삭제 시도
        // 이미 없어도 에러 없음 → 멱등성 보장
        likeRepository.deleteByUserIdAndProductId(userId, productId);
    }

    /**
     * 좋아요 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isLiked(String userId, Long productId) {
        return likeRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * 상품의 좋아요 수 조회
     */
    @Transactional(readOnly = true)
    public int getLikeCount(Long productId) {
        return likeRepository.countByProductId(productId);
    }

    /**
     * 사용자가 좋아요한 상품 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getLikedProductIds(String userId) {
        return likeRepository.findProductIdsByUserId(userId);
    }
}
