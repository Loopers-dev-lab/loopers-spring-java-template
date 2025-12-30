package com.loopers.domain.order.event;

import java.util.Objects;

public final class OrderEvent {

    private OrderEvent() {
    }

    public record PaymentRequested(
            Long orderId,
            String userId,
            String orderReference,
            String cardType,
            String cardNo,
            Long totalAmount,
            String callbackUrl
    ) {

        public PaymentRequested {
            Objects.requireNonNull(orderId, "orderId 는 null 일 수 없습니다.");
            Objects.requireNonNull(userId, "userId 는 null 일 수 없습니다.");
            Objects.requireNonNull(orderReference, "orderReference 는 null 일 수 없습니다.");
            Objects.requireNonNull(cardType, "cardType 은 null 일 수 없습니다.");
            Objects.requireNonNull(cardNo, "cardNo 는 null 일 수 없습니다.");
            Objects.requireNonNull(totalAmount, "totalAmount 는 null 일 수 없습니다.");
            Objects.requireNonNull(callbackUrl, "callbackUrl 은 null 일 수 없습니다.");
        }

        public static PaymentRequested of(
                Long orderId,
                String userId,
                String orderReference,
                String cardType,
                String cardNo,
                Long totalAmount,
                String callbackUrl
        ) {
            return new PaymentRequested(orderId, userId, orderReference, cardType, cardNo, totalAmount, callbackUrl);
        }
    }
}
