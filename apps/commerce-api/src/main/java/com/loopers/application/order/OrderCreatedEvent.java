package com.loopers.application.order;

import com.loopers.interfaces.api.payment.PaymentV1Dto;

public record OrderCreatedEvent(
        Long couponId,
        String orderId,
        PaymentV1Dto.CardTypeDto cardType,
        String cardNo,
        Long amount,
        String callbackUrl
) {
}
