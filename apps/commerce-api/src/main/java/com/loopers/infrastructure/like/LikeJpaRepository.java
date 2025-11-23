package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    @Query("""
        SELECT l
          FROM Like l
         WHERE l.userId = :userId
           AND l.deletedAt IS NULL
    """)
    List<Like> findActiveLikesByUserId(Long userId);
}
