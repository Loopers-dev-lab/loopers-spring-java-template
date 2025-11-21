package com.loopers.infrastructure.like;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.like.LikeEntity;
import com.loopers.domain.like.LikeRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@Component
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {
    private final LikeJpaRepository likeJpaRepository;

    @Override
    public LikeEntity save(LikeEntity entity) {
        return likeJpaRepository.save(entity);
    }

    @Override
    public Optional<LikeEntity> findByUserIdAndProductId(Long userId, Long productId) {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public List<LikeEntity> findAll() {
        return likeJpaRepository.findAll();
    }
}
