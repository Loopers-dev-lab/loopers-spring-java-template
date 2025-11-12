package com.loopers.domain.like;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeRepository {
    Like save(Like like);

    Optional<Like> findByUserIdAndProductId(String userId, Long productId);

    boolean existsByUserIdAndProductId(String userId, Long productId);

    void deleteByUserIdAndProductId(String userId, Long productId);

    Page<Like> findByUserId(String userId, Pageable pageable);

    Long countByProductId(Long productId);
}
