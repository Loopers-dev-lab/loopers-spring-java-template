package com.loopers.applications.streamer.consumer.product.dto;

import com.loopers.core.service.product.command.IncreaseProductSalesRankingScoreCommand;

public record IncreaseProductSalesRankingScoreEvent(
        String eventId,
        String paymentId
) {

    public IncreaseProductSalesRankingScoreCommand toCommand() {
        return new IncreaseProductSalesRankingScoreCommand(this.eventId, this.paymentId);
    }
}
