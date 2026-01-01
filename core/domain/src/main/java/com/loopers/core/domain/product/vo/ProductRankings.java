package com.loopers.core.domain.product.vo;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductRankings {

    private final List<ProductRanking> rankings;

    private final long totalElements;

    private final int totalPages;

    private final boolean hasNext;

    private final boolean hasPrevious;

    public ProductRankings(
            List<ProductRanking> rankings,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        this.rankings = rankings;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public List<ProductId> getProducts() {
        return this.rankings.stream()
                .map(ProductRanking::productId)
                .toList();
    }
}
