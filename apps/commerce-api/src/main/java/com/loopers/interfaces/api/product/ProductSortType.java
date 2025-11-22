package com.loopers.interfaces.api.product;

import java.util.Arrays;
import org.springframework.data.domain.Sort;

public enum ProductSortType {
  LATEST("latest", Sort.Direction.DESC, "id"),
  PRICE_ASC("price_asc", Sort.Direction.ASC, "price.value"),
  LIKES_DESC("likes_desc", Sort.Direction.DESC, "likeCount");

  private final String parameterValue;
  private final Sort.Direction direction;
  private final String property;

  ProductSortType(String parameterValue, Sort.Direction direction, String property) {
    this.parameterValue = parameterValue;
    this.direction = direction;
    this.property = property;
  }

  public Sort toSort() {
    return Sort.by(direction, property);
  }

  public static ProductSortType from(String parameterValue) {
    if (parameterValue == null) {
      return LATEST;
    }

    return Arrays.stream(values())
        .filter(type -> type.parameterValue.equalsIgnoreCase(parameterValue))
        .findFirst()
        .orElse(LATEST);
  }
}
