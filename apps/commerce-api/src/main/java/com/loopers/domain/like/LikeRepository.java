package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    boolean save(Like like);

    List<Like> findByUserId(Long userId);

    boolean delete(Like like);

    List<Like> findAllByUserId(Long userId);
}
