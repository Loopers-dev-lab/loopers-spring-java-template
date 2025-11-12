package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.productlike.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductLikeRepositoryImpl implements ProductLikeRepository {

  private final ProductLikeJpaRepository jpaRepository;

  @Override
  public boolean existsByUserIdAndProductId(Long userId, Long productId) {
    if (userId == null) {
      return false;
    }
    return jpaRepository.existsByUserIdAndProductId(userId, productId);
  }

  @Override
  public List<ProductLike> findByUserIdAndProductIdIn(Long userId, List<Long> productIds) {
    if (userId == null || productIds == null || productIds.isEmpty()) {
      return List.of();
    }
    return jpaRepository.findByUserIdAndProductIdIn(userId, productIds);
  }

  @Override
  public ProductLike save(ProductLike productLike) {
    return jpaRepository.save(productLike);
  }

  @Override
  public void deleteByUserIdAndProductId(Long userId, Long productId) {
    jpaRepository.deleteByUserIdAndProductId(userId, productId);
  }
}
