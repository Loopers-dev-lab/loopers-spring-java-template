package com.loopers.domain.product;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record ProductSearchCondition(
        Long brandId,
        Pageable pageable
) {
    public PageRequest toPageRequest() {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );
    }
}
