package com.loopers.core.service.product.command;

public record ClearProductCacheCommand(
        String eventId,
        String productId
) {
}
