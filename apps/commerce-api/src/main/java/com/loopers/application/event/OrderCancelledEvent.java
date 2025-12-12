package com.loopers.application.event;

public record OrderCancelledEvent(
    Long orderId,
    Long userId,
    String reason
) {}