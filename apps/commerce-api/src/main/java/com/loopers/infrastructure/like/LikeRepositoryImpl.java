package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LikeRepository의 JPA 구현체.
 */
@RequiredArgsConstructor
@Repository
public class LikeRepositoryImpl implements LikeRepository {
    private final LikeJpaRepository likeJpaRepository;

    @Override
    public Like save(Like like) {
        return likeJpaRepository.save(like);
    }

    @Override
    public Optional<Like> findByUserIdAndProductId(Long userId, Long productId) {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public void delete(Like like) {
        likeJpaRepository.delete(like);
    }

    @Override
    public List<Like> findAllByUserId(Long userId) {
        return likeJpaRepository.findAllByUserId(userId);
    }

    /**
     * Counts likes for the specified product IDs.
     *
     * @param productIds the product IDs to aggregate like counts for
     * @return a map from product ID to its like count
     */
    @Override
    public Map<Long, Long> countByProductIds(List<Long> productIds) {
        return likeJpaRepository.countByProductIdsAsMap(productIds);
    }

    /**
     * Produces a map of product IDs to their total like counts.
     *
     * @return a {@code Map<Long, Long>} where each key is a product ID and each value is the number of likes for that product
     */
    @Override
    public Map<Long, Long> countAllByProductIds() {
        return likeJpaRepository.countAllByProductIds().stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Number) row[1]).longValue()
            ));
    }
}
