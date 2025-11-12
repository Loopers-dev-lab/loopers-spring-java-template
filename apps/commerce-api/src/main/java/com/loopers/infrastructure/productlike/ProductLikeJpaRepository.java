package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {


  Optional<ProductLike> findByUserIdAndProductId(Long userId, Long productId);


  boolean existsByUserIdAndProductId(Long userId, Long productId);


  List<ProductLike> findByUserIdAndProductIdIn(Long userId, List<Long> productIds);

  void deleteByUserIdAndProductId(Long userId, Long productId);
}
