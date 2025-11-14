package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  List<ProductLike> findByUserIdAndProductIdIn(Long userId, List<Long> productIds);

  void deleteByUserIdAndProductId(Long userId, Long productId);
}
