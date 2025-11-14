package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    int countByProductId(Long productId);
}
