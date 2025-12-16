package com.loopers.applications.streamer.consumer.product.dto;

import com.loopers.core.service.product.command.ClearProductCacheCommand;

public record ClearProductCacheEvent(
        String eventId,
        String productId
) {

    public ClearProductCacheCommand toCommand() {
        return new ClearProductCacheCommand(this.eventId, this.productId);
    }
}
