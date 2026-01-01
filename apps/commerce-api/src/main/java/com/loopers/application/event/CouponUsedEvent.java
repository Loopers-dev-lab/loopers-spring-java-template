package com.loopers.application.event;

import com.loopers.domain.order.Money;
import com.loopers.domain.payment.CardType;

public record CouponUsedEvent(
    String orderId,
    Long userId,
    Long couponIssueId,
    Money totalPrice,
    CardType cardType,
    String cardNo
) {
}
