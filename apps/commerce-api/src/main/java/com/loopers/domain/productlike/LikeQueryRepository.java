package com.loopers.domain.productlike;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeQueryRepository {

  Page<LikedProduct> findByUserIdOrderByLatest(Long userId, Pageable pageable);

  Page<LikedProduct> findByUserIdOrderByProductName(Long userId, Pageable pageable);

  Page<LikedProduct> findByUserIdOrderByPriceAsc(Long userId, Pageable pageable);

  Page<LikedProduct> findByUserIdOrderByPriceDesc(Long userId, Pageable pageable);
}
