package com.loopers.domain.productlike;

import java.util.List;

public interface ProductLikeRepository {

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  List<ProductLike> findByUserIdAndProductIdIn(Long userId, List<Long> productIds);

  ProductLike save(ProductLike productLike);

  ProductLike saveAndFlush(ProductLike productLike);

  int deleteByUserIdAndProductId(Long userId, Long productId);
}
