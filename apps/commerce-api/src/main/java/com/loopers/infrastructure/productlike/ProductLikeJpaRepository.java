package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  @Modifying
  @Query(nativeQuery = true,
      value = "INSERT IGNORE INTO product_like (ref_user_id, ref_product_id, liked_at, created_at, updated_at) " +
              "VALUES (:userId, :productId, :likedAt, NOW(), NOW())")
  int insertIgnore(@Param("userId") Long userId,
                   @Param("productId") Long productId,
                   @Param("likedAt") LocalDateTime likedAt);

  List<ProductLike> findByUserIdAndProductIdIn(Long userId, List<Long> productIds);

  @Modifying
  @Query("DELETE FROM ProductLike pl WHERE pl.userId = :userId AND pl.productId = :productId")
  int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

}
