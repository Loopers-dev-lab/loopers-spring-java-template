package com.loopers.core.service.product.command;

public record IncreaseProductSalesRankingScoreCommand(
        String eventId,
        String paymentId
) {
}
