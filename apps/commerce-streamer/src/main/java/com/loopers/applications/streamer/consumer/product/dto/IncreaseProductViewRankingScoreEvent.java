package com.loopers.applications.streamer.consumer.product.dto;

import com.loopers.core.service.product.command.IncreaseProductViewRankingScoreCommand;

public record IncreaseProductViewRankingScoreEvent(String eventId, String productId) {

    public IncreaseProductViewRankingScoreCommand toCommand() {
        return new IncreaseProductViewRankingScoreCommand(this.eventId, this.productId);
    }
}
