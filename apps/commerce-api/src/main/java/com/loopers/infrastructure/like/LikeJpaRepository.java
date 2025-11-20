package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Transactional
    @Modifying
    @Query(
            value = "INSERT IGNORE INTO likes (user_id, product_id, created_at, updated_at) " +
                    "VALUES (:userId, :productId, NOW(), NOW()) ",
            nativeQuery = true
    )
    int saveIgnore(@Param("userId") Long userId, @Param("productId") Long productId);

    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);
}
