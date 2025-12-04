package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
public interface LikeRepository {
    LikeEntity save(LikeEntity entity);

    Optional<LikeEntity> findByUserIdAndProductId(Long userId, Long productId);

    List<LikeEntity> findAll();

    /**
     * 특정 상품의 활성 좋아요 수를 조회한다.
     */
    Long countByProductIdAndDeletedAtIsNull(Long productId);

    /**
     * 좋아요가 있는 모든 상품 ID를 조회한다.
     *
     * 배치 동기화 시 사용됩니다.
     */
    List<Long> findDistinctProductIds();
}
