package com.loopers.batch.dto;

public record ProductMetricsAgg(
        Long productId,
        long sumLike,
        long sumView,
        long sumSales
) {
}
