package com.loopers.domain.productlike;

import com.loopers.domain.event.Events;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnlikedEvent;
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

  //userId는 null일수 있음 (비회원)
  public Map<Long, Boolean> findLikeStatusByProductId(Long userId, List<Long> productIds) {
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

  @Transactional
  public boolean createLike(Long userId, Long productId) {
    LocalDateTime likedAt = LocalDateTime.now(clock);
    ProductLike like = ProductLike.of(userId, productId, likedAt);
    boolean saved = productLikeRepository.saveIfNotExists(like);

    if (saved) {
      Events.raise(ProductLikedEvent.of(userId, productId, likedAt));
    }

    return saved;
  }

  @Transactional
  public int deleteLike(Long userId, Long productId) {
    int deleted = productLikeRepository.deleteByUserIdAndProductId(userId, productId);

    if (deleted > 0) {
      LocalDateTime unlikedAt = LocalDateTime.now(clock);
      Events.raise(ProductUnlikedEvent.of(userId, productId, unlikedAt));
    }

    return deleted;
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
