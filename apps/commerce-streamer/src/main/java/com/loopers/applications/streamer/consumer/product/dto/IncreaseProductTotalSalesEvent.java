package com.loopers.applications.streamer.consumer.product.dto;

import com.loopers.core.service.product.command.IncreaseProductTotalSalesCommand;

public record IncreaseProductTotalSalesEvent(String eventId, String paymentId) {

    public IncreaseProductTotalSalesCommand toCommand() {
        return new IncreaseProductTotalSalesCommand(this.eventId, this.paymentId);
    }
}
