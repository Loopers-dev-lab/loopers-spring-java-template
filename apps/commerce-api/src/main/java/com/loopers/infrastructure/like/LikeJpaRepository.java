package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    List<Like> findByUserId(Long userId);

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO `likes` (user_id, product_id, created_at, updated_at)
        VALUES (:userId, :productId, NOW(), NOW())
    """, nativeQuery = true)
    int insertIgnore(Long userId, Long productId);

    @Modifying
    @Query(value = """
        DELETE FROM `likes`
         WHERE user_id = :userId
           AND product_id = :productId
    """, nativeQuery = true)
    int delete(Long userId, Long productId);
}
