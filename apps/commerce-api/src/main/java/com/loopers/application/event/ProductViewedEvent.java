package com.loopers.application.event;

public record ProductViewedEvent(
    Long userId,
    Long productId
) {
}