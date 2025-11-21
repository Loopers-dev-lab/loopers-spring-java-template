package com.loopers.infrastructure.like;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.like.LikeEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
public interface LikeJpaRepository extends JpaRepository<LikeEntity, Long> {
    Optional<LikeEntity> findByUserIdAndProductId(Long userId, Long productId);
}
