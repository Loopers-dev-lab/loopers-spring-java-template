package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 좋아요 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    @Override
    public Like save(Like like) {
        LikeEntity entity = LikeEntity.from(like);
        LikeEntity saved = likeJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Like> findByUserIdAndProductId(String userId, Long productId) {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId)
                .map(LikeEntity::toDomain);
    }

    @Override
    public boolean existsByUserIdAndProductId(String userId, Long productId) {
        return likeJpaRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public void deleteByUserIdAndProductId(String userId, Long productId) {
        likeJpaRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    public int countByProductId(Long productId) {
        return likeJpaRepository.countByProductId(productId);
    }

    @Override
    public List<Long> findProductIdsByUserId(String userId) {
        return likeJpaRepository.findProductIdsByUserId(userId);
    }
}
