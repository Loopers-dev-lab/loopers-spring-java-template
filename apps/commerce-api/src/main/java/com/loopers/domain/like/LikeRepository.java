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
}
