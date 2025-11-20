package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndProductId(String userId, Long productId);
    Long countByProductId(Long productId);
    List<Like> findAllByUserId(String userId);
}
