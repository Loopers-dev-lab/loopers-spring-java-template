package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.productlike.ProductLikeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    return jpaRepository.saveAndFlush(productLike);
  }

  @Override
  public boolean saveAndFlush(ProductLike productLike) {
    int result = jpaRepository.insertIgnore(
        productLike.getUserId(),
        productLike.getProductId(),
        productLike.getLikedAt()
    );
    return result > 0;
  }

  @Override
  public int deleteByUserIdAndProductId(Long userId, Long productId) {
    return jpaRepository.deleteByUserIdAndProductId(userId, productId);
  }
}
