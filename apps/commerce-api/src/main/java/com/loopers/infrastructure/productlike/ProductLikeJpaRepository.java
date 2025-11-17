package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  List<ProductLike> findByUserIdAndProductIdIn(Long userId, List<Long> productIds);

  @Modifying
  @Query("DELETE FROM ProductLike pl WHERE pl.userId = :userId AND pl.productId = :productId")
  int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
