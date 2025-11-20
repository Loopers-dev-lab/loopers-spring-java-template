package com.loopers.domain.like;

import java.util.Optional;

public interface LikeRepository {
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    int saveIgnore(final Long userId, final Long productId);

    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    void delete(Like like);
}
