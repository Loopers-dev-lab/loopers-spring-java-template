package com.loopers.domain.product;

import org.springframework.data.domain.Sort;

public enum ProductListSortType {

  LATEST("id", Sort.Direction.DESC),
  LIKES_DESC("likeCount", Sort.Direction.DESC),
  PRICE_ASC("price.value", Sort.Direction.ASC);

  private final String property;
  private final Sort.Direction direction;

  ProductListSortType(String property, Sort.Direction direction) {
    this.property = property;
    this.direction = direction;
  }

  public Sort toSort() {
    return Sort.by(direction, property);
  }

  public String getCacheKey() {
    return name().toLowerCase();
  }
}
