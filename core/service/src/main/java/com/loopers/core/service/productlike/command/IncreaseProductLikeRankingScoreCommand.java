package com.loopers.core.service.productlike.command;

public record IncreaseProductLikeRankingScoreCommand(
        String eventId,
        String productId
) {
}
