package com.loopers.application.metrics;

import com.loopers.interfaces.dto.ProductLikePayload;
import com.loopers.interfaces.dto.ProductStockPayload;
import com.loopers.interfaces.dto.ProductViewPayload;

public record ProductMetricsCommand(
        String eventId,
        Long productId,
        MetricsType metricsType,
        String likeType,
        Integer stock,
        String changedType
) {
    public static ProductMetricsCommand from(ProductLikePayload payload) {
        return new ProductMetricsCommand(
                payload.eventId(),
                payload.productId(),
                MetricsType.LIKE,
                payload.likeType(),
                null,
                null
        );
    }

    public static ProductMetricsCommand from(ProductStockPayload payload) {
        return new ProductMetricsCommand(
                payload.eventId(),
                payload.productId(),
                MetricsType.STOCK,
                null,
                payload.stock(),
                payload.changedType()
        );
    }

    public static ProductMetricsCommand from(ProductViewPayload payload) {
        return new ProductMetricsCommand(
                payload.eventId(),
                payload.productId(),
                MetricsType.VIEW,
                null,
                null,
                null
        );
    }
}
