package com.loopers.applications.streamer.consumer.product.dto;

import com.loopers.core.service.product.command.IncreaseProductLikeMetricCommand;

public record IncreaseProductLikeMetricEvent(
        String eventId,
        String productId
) {
    public IncreaseProductLikeMetricCommand toCommand() {
        return new IncreaseProductLikeMetricCommand(this.eventId, this.productId);
    }
}
