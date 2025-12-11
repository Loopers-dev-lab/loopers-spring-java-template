package com.loopers.application.event;

import com.loopers.domain.payment.CardType;

public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    Long couponIssueId,
    CardType cardType,
    String cardNo
) {
}
