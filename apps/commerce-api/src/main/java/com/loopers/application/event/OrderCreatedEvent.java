package com.loopers.application.event;

import com.loopers.domain.payment.CardType;
import java.util.List;

public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    Long couponIssueId,
    CardType cardType,
    String cardNo,
    List<OrderItemData> orderItems
) {
    
    public record OrderItemData(
        Long productId,
        Long quantity,
        Long price
    ) {}
}
