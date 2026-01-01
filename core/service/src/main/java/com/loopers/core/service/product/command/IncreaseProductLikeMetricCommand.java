package com.loopers.core.service.product.command;

public record IncreaseProductLikeMetricCommand(
        String eventId,
        String productId
) {
}
