package com.loopers.core.service.product.command;

public record IncreaseProductViewRankingScoreCommand(
        String eventId,
        String productId
) {
}
