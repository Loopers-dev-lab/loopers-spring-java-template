package com.loopers.domain.productlike;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(productIds, "productIds는 null일 수 없습니다.");

    List<Long> distinctProductIds = productIds.stream()
        .distinct()
        .toList();

    List<ProductLike> likes = productLikeRepository.findByUserIdAndProductIdIn(userId, distinctProductIds);

    Map<Long, Boolean> likedByProductId = likes.stream()
        .collect(Collectors.toMap(ProductLike::getProductId, like -> true));

    return distinctProductIds.stream()
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
    Objects.requireNonNull(sortType, "sortType은 null일 수 없습니다.");

    return switch (sortType) {
      case LATEST -> likeQueryRepository.findByUserIdOrderByLatest(userId, pageable);
      case PRODUCT_NAME -> likeQueryRepository.findByUserIdOrderByProductName(userId, pageable);
      case PRICE_ASC -> likeQueryRepository.findByUserIdOrderByPriceAsc(userId, pageable);
      case PRICE_DESC -> likeQueryRepository.findByUserIdOrderByPriceDesc(userId, pageable);
    };
  }
}
