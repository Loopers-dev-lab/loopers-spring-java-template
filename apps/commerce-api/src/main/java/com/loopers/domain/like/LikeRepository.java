package com.loopers.domain.like;

import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository {
    void saveLike(Like like);

    void delete(Long userId, Long productId);

    boolean saveIfAbsent(Long userId, Long productId);

    boolean deleteIfPresent(Long userId, Long productId);

    List<Long> findProductIdsByUserId(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
