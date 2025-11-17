package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT l.productId FROM Like l WHERE l.userId = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Like l WHERE l.userId = :userId AND l.productId = :productId")
    int deleteByUserIdAndProductIdAndReturnCount(@Param("userId") Long userId, @Param("productId") Long productId);
}

