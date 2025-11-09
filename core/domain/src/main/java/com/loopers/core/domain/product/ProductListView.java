package com.loopers.core.domain.product;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductListView {

    private final List<ProductListItem> items;

    private final long totalElements;

    private final int totalPages;

    private final boolean hasNext;

    private final boolean hasPrevious;

    public ProductListView(
            List<ProductListItem> items,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        this.items = items;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }
}
