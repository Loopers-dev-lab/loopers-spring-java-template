package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {
  Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

  @Modifying
  @Query(value = "INSERT IGNORE INTO likes (ref_user_id, ref_product_id, created_at, updated_at) VALUES (:userId, :productId, NOW(), NOW())", nativeQuery = true)
  int save(@Param("userId") Long userId, @Param("productId") Long productId);

  @Modifying
  @Query("delete from Like l where l.user.id = :userId and l.product.id = :productId")
  void delete(@Param("userId") Long userId, @Param("productId") Long productId);

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  long countByProductId(Long productId);

  @Query(
      value = "SELECT l FROM Like l JOIN FETCH l.product p WHERE l.user.id = :userId",
      countQuery = "SELECT COUNT(l) FROM Like l WHERE l.user.id = :userId"
  )
  Page<Like> getLikedProducts(@Param("userId") Long userId, Pageable pageable);
}
