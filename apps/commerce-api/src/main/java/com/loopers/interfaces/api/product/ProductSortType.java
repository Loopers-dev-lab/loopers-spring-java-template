package com.loopers.interfaces.api.product;

import com.loopers.domain.product.ProductListSortType;
import com.loopers.domain.product.ProductSearchCondition;
import org.springframework.data.domain.Sort;

public enum ProductSortType {

  LATEST(ProductListSortType.LATEST),
  PRICE_ASC(ProductListSortType.PRICE_ASC),
  LIKES_DESC(ProductListSortType.LIKES_DESC);

  private final ProductListSortType domainType;

  ProductSortType(ProductListSortType domainType) {
    this.domainType = domainType;
  }

  public ProductSearchCondition toCondition(Long userId, int page, int size) {
    return ProductSearchCondition.of(userId, page, size, domainType);
  }

  public Sort toSort() {
    return domainType.toSort();
  }
}
