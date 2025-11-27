package com.loopers.application.product;

import com.loopers.domain.product.ProductSortType;
import java.util.Objects;

public record ProductSearchCriteria(
        Long brandId,
        ProductSortType productSortType,
        Integer page,
        Integer size
) {
    public ProductSearchCriteria {
        if (Objects.isNull(page)) page = 1;
        if (Objects.isNull(size)) size = 20;
    }
}
