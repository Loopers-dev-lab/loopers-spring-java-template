package com.loopers.applications.streamer.consumer.product.dto;

import com.loopers.core.service.productlike.command.IncreaseProductLikeRankingScoreCommand;

public record IncreaseProductLikeRankingScoreEvent(
        String eventId,
        String productId
) {

    public IncreaseProductLikeRankingScoreCommand toCommand() {
        return new IncreaseProductLikeRankingScoreCommand(this.eventId, this.productId);
    }
}
