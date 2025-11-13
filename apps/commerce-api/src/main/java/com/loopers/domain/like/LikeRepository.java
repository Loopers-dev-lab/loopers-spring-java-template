package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {

    /**
     * 좋아요 저장
     */
    Like save(Like like);

    /**
     * 특정 사용자의 특정 상품 좋아요 조회
     */
    Optional<Like> findByUserIdAndProductId(String userId, Long productId);

    /**
     * 좋아요 존재 여부 확인 (중복 체크용)
     */
    boolean existsByUserIdAndProductId(String userId, Long productId);

    /**
     * 좋아요 삭제 (취소)
     */
    void deleteByUserIdAndProductId(String userId, Long productId);

    /**
     * 특정 상품의 좋아요 수 집계
     */
    int countByProductId(Long productId);

    /**
     * 특정 사용자가 좋아요한 상품 ID 목록
     */
    List<Long> findProductIdsByUserId(String userId);
}
