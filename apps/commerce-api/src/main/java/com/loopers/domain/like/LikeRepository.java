package com.loopers.domain.like;

import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    Like save(Like like);

    void delete(Like like);

    int countByProductId(Long productId);
}
