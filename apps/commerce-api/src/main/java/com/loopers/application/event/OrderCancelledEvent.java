package com.loopers.application.event;

public record OrderCancelledEvent(
    String orderId,
    Long userId,
    String reason
) {}
