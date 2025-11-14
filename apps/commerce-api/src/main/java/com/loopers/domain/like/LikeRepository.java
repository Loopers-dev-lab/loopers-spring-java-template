package com.loopers.domain.like;

import java.util.Optional;

public interface LikeRepository {

    Optional<Like> save(Like like);
    long deleteByUserIdAndLikeTargetId(Long userId, Long likeTargetId, LikeTargetType likeTargetType);
    Optional<Like> findByUserIdAndLikeTargetId(Long userId, Long likeTargetId, LikeTargetType likeTargetType);
    
    /**
     * 좋아요 등록 (동시성 안전)
     * INSERT ... ON DUPLICATE KEY UPDATE를 사용하여 원자적으로 처리
     */
    long insertOrIgnore(Long userId, Long likeTargetId, LikeTargetType likeTargetType);
}

