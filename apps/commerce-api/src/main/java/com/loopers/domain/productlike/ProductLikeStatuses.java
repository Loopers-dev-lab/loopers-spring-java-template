package com.loopers.domain.productlike;

import java.util.Map;

public class ProductLikeStatuses {

  private final Map<Long, Boolean> values;

  private ProductLikeStatuses(Map<Long, Boolean> values) {
    this.values = values;
  }

  public static ProductLikeStatuses from(Map<Long, Boolean> statuses) {
    return new ProductLikeStatuses(Map.copyOf(statuses));
  }

  public static ProductLikeStatuses empty() {
    return new ProductLikeStatuses(Map.of());
  }

  public boolean isLiked(Long productId) {
    return values.getOrDefault(productId, false);
  }

  public Map<Long, Boolean> toMap() {
    return Map.copyOf(values);
  }
}
