package com.loopers.application.order;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentType;

import java.util.List;

public record OrderCommand(
        String userId,
        List<OrderItemCommand> items,
        Long couponId,
        PaymentType paymentType,
        CardType cardType,
        String cardNo
) {
    public record OrderItemCommand(
            Long productId,
            Integer quantity
    ) {
    }
}
