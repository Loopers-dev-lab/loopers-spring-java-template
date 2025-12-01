package com.loopers.domain.product;

import java.util.Set;

public record ProductSearchCondition(
    Long userId,
    int pageNumber,
    int pageSize,
    ProductListSortType sortType
) {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final Set<ProductListSortType> CACHEABLE_SORT_TYPES = Set.of(
      ProductListSortType.LATEST,
      ProductListSortType.LIKES_DESC
  );

  public static ProductSearchCondition of(Long userId, int page, int size, ProductListSortType sortType) {
    return new ProductSearchCondition(userId, page, size, sortType);
  }

  public boolean isCacheable() {
    return userId == null
        && pageNumber == DEFAULT_PAGE
        && pageSize == DEFAULT_SIZE
        && CACHEABLE_SORT_TYPES.contains(sortType);
  }
}
