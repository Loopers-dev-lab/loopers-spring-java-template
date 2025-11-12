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

  public Map<Long, Boolean> toLikedMap() {
    return values.stream()
        .collect(Collectors.toMap(ProductLike::getProductId, like -> true));
  }

  public ProductLikeStatuses toStatuses(List<Long> allProductIds) {
    if (allProductIds == null || allProductIds.isEmpty()) {
      return ProductLikeStatuses.empty();
    }

    Map<Long, Boolean> likedMap = toLikedMap();

    Map<Long, Boolean> statusMap = allProductIds.stream()
        .collect(Collectors.toMap(
            productId -> productId,
            productId -> likedMap.getOrDefault(productId, false)
        ));

    return ProductLikeStatuses.from(statusMap);
  }
}
