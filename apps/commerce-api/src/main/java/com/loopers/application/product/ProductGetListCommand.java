package com.loopers.application.product;

import com.loopers.domain.product.ProductSortType;
import org.springframework.data.domain.Pageable;

public record ProductGetListCommand(
        Long brandId,
        String sort,
        Pageable pageable
) {
    public ProductSortType getSortType() {
        return ProductSortType.from(sort);
    }
}
