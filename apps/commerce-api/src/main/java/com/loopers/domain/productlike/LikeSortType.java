package com.loopers.domain.productlike;

import java.util.Arrays;

public enum LikeSortType {
  LATEST("latest", "likedAt: DESC"),
  PRODUCT_NAME("product_name", "productName: ASC"),
  PRICE_ASC("price_asc", "price: ASC"),
  PRICE_DESC("price_desc", "price: DESC");

  private final String parameterValue;
  private final String sortDescription;

  LikeSortType(String parameterValue, String sortDescription) {
    this.parameterValue = parameterValue;
    this.sortDescription = sortDescription;
  }

  public String parameterValue() {
    return parameterValue;
  }

  public String sortDescription() {
    return sortDescription;
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
