package com.loopers.applications.streamer.consumer.product.dto;

import com.loopers.core.service.product.command.IncreaseProductMetricViewCountCommand;

public record IncreaseProductViewEvent(String eventId, String productId) {

    public IncreaseProductMetricViewCountCommand toCommand() {
        return new IncreaseProductMetricViewCountCommand(this.eventId, this.productId);
    }
}
