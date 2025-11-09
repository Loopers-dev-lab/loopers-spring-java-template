package com.loopers.core.domain.product;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductListView {

    private final List<Product> products;

    private final long totalElements;

    private final int totalPages;

    private final boolean hasNext;

    private final boolean hasPrevious;

    public ProductListView(
            List<Product> products,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        this.products = products;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }
}
