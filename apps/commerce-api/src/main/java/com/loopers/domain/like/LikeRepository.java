package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);
    Like save(Like like);

    List<Like> findActiveLikesByUserId(Long userId);
}
