package com.loopers.domain.productlike;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductLikes {

  private final List<ProductLike> values;

  private ProductLikes(List<ProductLike> productLikes) {
    this.values = List.copyOf(productLikes);
  }

  public static ProductLikes from(List<ProductLike> productLikes) {
    if (productLikes == null) {
      throw new IllegalArgumentException("productLikes cannot be null");
    }
    return new ProductLikes(productLikes);
  }

  public static ProductLikes empty() {
    return new ProductLikes(List.of());
  }

  // 중복 productId가 있으면 IllegalStateException 발생 (데이터 무결성 오류)
  public Map<Long, Boolean> toLikedByProductId() {
    return values.stream()
        .collect(Collectors.toMap(ProductLike::getProductId, like -> true));
  }

  public Map<Long, Boolean> toLikeStatusByProductId(List<Long> allProductIds) {
    if (allProductIds == null || allProductIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, Boolean> likedByProductId = toLikedByProductId();

    // allProductIds에 중복이 있으면 IllegalStateException 발생
    return allProductIds.stream()
        .collect(Collectors.toMap(
            productId -> productId,
            productId -> likedByProductId.getOrDefault(productId, false)
        ));
  }
}
