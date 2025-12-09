package com.loopers.domain.stock.event;

public record StockProcessingFailedEvent(
    Long orderId,
    String reason
) {
}