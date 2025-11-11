package com.loopers.core.domain.order;

import lombok.Getter;

import java.util.List;

@Getter
public class OrderListView {

    private final List<OrderListItem> items;

    private final long totalElements;

    private final int totalPages;

    private final boolean hasNext;

    private final boolean hasPrevious;

    public OrderListView(
            List<OrderListItem> items,
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
