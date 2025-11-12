package com.loopers.domain.productlike;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLikeService {

  private final ProductLikeRepository productLikeRepository;

  public boolean isLiked(Long userId, Long productId) {
    return productLikeRepository.existsByUserIdAndProductId(userId, productId);
  }


  public ProductLikeStatuses findLikeStatusByUser(Long userId, List<Long> productIds) {
    if (userId == null || productIds == null || productIds.isEmpty()) {
      return ProductLikeStatuses.empty();
    }

    List<ProductLike> likes = productLikeRepository.findByUserIdAndProductIdIn(userId, productIds);
    ProductLikes productLikes = ProductLikes.from(likes);

    return productLikes.toStatuses(productIds);
  }


  @Transactional
  public void createLike(Long userId, Long productId) {
    if (productLikeRepository.existsByUserIdAndProductId(userId, productId)) {
      return;
    }

    ProductLike like = ProductLike.of(userId, productId, LocalDateTime.now());
    productLikeRepository.save(like);
  }


  @Transactional
  public void deleteLike(Long userId, Long productId) {
    productLikeRepository.deleteByUserIdAndProductId(userId, productId);
  }
}
