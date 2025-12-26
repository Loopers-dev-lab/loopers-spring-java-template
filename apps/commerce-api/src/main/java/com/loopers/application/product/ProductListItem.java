package com.loopers.application.product;

import java.math.BigDecimal;

public record ProductListItem(Long id, String name, BigDecimal price, long likeCount, Integer rank) {
  public static ProductListItem from(Long id, String name, BigDecimal price, long likeCount, Integer rank) {
    return new ProductListItem(
        id,
        name,
        price,
        likeCount,
        rank
    );
  }
}
