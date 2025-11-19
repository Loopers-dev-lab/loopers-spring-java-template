package com.loopers.domain.productlike;

import java.util.Arrays;

public enum LikeSortType {
  LATEST("latest"),
  PRODUCT_NAME("product_name"),
  PRICE_ASC("price_asc"),
  PRICE_DESC("price_desc");

  private final String parameterValue;

  LikeSortType(String parameterValue) {
    this.parameterValue = parameterValue;
  }

  public String parameterValue() {
    return parameterValue;
  }

  public static LikeSortType from(String parameterValue) {
    if (parameterValue == null) {
      return LATEST;
    }

    return Arrays.stream(values())
        .filter(type -> type.parameterValue.equalsIgnoreCase(parameterValue))
        .findFirst()
        .orElse(LATEST);
  }
}
