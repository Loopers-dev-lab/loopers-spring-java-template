package com.loopers.domain.productlike;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductLikeService {

  private final ProductLikeRepository productLikeRepository;
  private final Clock clock;

  public boolean isLiked(Long userId, Long productId) {
    if (userId == null || productId == null) {
      return false;
    }
    return productLikeRepository.existsByUserIdAndProductId(userId, productId);
  }


  public Map<Long, Boolean> findLikeStatusByProductId(Long userId, List<Long> productIds) {
    if (userId == null || productIds == null || productIds.isEmpty()) {
      return Map.of();
    }

    List<ProductLike> likes = productLikeRepository.findByUserIdAndProductIdIn(userId, productIds);
    ProductLikes productLikes = ProductLikes.from(likes);

    return productLikes.toLikeStatusByProductId(productIds);
  }


  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createLike(Long userId, Long productId) {
      ProductLike like = ProductLike.of(userId, productId, LocalDateTime.now(clock));
      productLikeRepository.saveAndFlush(like);
  }

  @Transactional
  public int deleteLike(Long userId, Long productId) {
    return productLikeRepository.deleteByUserIdAndProductId(userId, productId);
  }
}
