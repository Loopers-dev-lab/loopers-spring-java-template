package com.loopers.domain.product;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record ProductSearchCondition(
        Long brandId,
        ProductSortType sortType,
        Pageable pageable
) {
    public PageRequest toPageRequest() {
        Sort sort = sortType != null ? sortType.getSort() : ProductSortType.LATEST.getSort();

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }
}
