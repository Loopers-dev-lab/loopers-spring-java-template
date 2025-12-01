package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.LikeQueryRepository;
import com.loopers.domain.productlike.LikedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeQueryRepositoryImpl implements LikeQueryRepository {

  private final LikeQueryJpaRepository jpaRepository;

  @Override
  public Page<LikedProduct> findByUserIdOrderByLatest(Long userId, Pageable pageable) {
    return jpaRepository.findByUserIdOrderByLatest(userId, pageable);
  }

  @Override
  public Page<LikedProduct> findByUserIdOrderByProductName(Long userId, Pageable pageable) {
    return jpaRepository.findByUserIdOrderByProductName(userId, pageable);
  }

  @Override
  public Page<LikedProduct> findByUserIdOrderByPriceAsc(Long userId, Pageable pageable) {
    return jpaRepository.findByUserIdOrderByPriceAsc(userId, pageable);
  }

  @Override
  public Page<LikedProduct> findByUserIdOrderByPriceDesc(Long userId, Pageable pageable) {
    return jpaRepository.findByUserIdOrderByPriceDesc(userId, pageable);
  }
}
