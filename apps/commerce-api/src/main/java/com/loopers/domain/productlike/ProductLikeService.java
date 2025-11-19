package com.loopers.domain.productlike;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductLikeService {

  private final ProductLikeRepository productLikeRepository;
  private final LikeQueryRepository likeQueryRepository;
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
    if (likes.isEmpty()) {
      return Map.of();
    }

    Map<Long, Boolean> likedByProductId = likes.stream()
        .collect(Collectors.toMap(ProductLike::getProductId, like -> true));

    // allProductIds에 중복이 있으면 IllegalStateException 발생
    return productIds.stream()
        .collect(Collectors.toMap(
            productId -> productId,
            productId -> likedByProductId.getOrDefault(productId, false)
        ));
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

  public Page<LikedProduct> findLikedProducts(Long userId, LikeSortType sortType,
      Pageable pageable) {
    return switch (sortType) {
      case LATEST -> likeQueryRepository.findByUserIdOrderByLatest(userId, pageable);
      case PRODUCT_NAME -> likeQueryRepository.findByUserIdOrderByProductName(userId, pageable);
      case PRICE_ASC -> likeQueryRepository.findByUserIdOrderByPriceAsc(userId, pageable);
      case PRICE_DESC -> likeQueryRepository.findByUserIdOrderByPriceDesc(userId, pageable);
    };
  }
}
