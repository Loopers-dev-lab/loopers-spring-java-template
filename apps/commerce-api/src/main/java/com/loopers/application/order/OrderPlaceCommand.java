package com.loopers.application.order;

import java.util.List;

public record OrderPlaceCommand(
        String userId,
        List<OrderItemCommand> items,
        Long couponId,
        PaymentMethod paymentMethod,
        CardInfo cardInfo
) {
    public record OrderItemCommand(
            Long productId,
            Integer quantity
    ) {
    }

    public record CardInfo(
            String cardType,
            String cardNo
    ) {
    }

    public enum PaymentMethod {
        POINT,
        PG_CARD
    }

    public List<Long> getSortedProductIds() {
        return items.stream()
                .map(OrderItemCommand::productId)
                .sorted()
                .distinct()
                .toList();
    }
}

