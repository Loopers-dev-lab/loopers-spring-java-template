package com.loopers.core.domain.productlike;

import lombok.Getter;

import java.util.List;

@Getter
public class LikeProductListView {

    private final List<LikeProductListItem> items;

    private final long totalElements;

    private final int totalPages;

    private final boolean hasNext;

    private final boolean hasPrevious;

    public LikeProductListView(
            List<LikeProductListItem> items,
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
