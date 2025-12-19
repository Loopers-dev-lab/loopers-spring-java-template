package com.loopers.application.order;

import com.loopers.interfaces.api.order.OrderDto;

import java.util.List;

public record OrderCreateCommand(
    String userId,
    List<OrderDto.OrderItemRequest> items,
    CardInfo cardInfo,
    Long couponId
) {
    public static OrderCreateCommand forPointPayment(String userId, List<OrderDto.OrderItemRequest> items) {
        return new OrderCreateCommand(userId, items, null, null);
    }

    public static OrderCreateCommand forPointPaymentWithCoupon(String userId, List<OrderDto.OrderItemRequest> items, Long couponId) {
        return new OrderCreateCommand(userId, items, null, couponId);
    }

    public static OrderCreateCommand forCardPayment(String userId, List<OrderDto.OrderItemRequest> items, CardInfo cardInfo) {
        return new OrderCreateCommand(userId, items, cardInfo, null);
    }

    public static OrderCreateCommand forCardPaymentWithCoupon(String userId, List<OrderDto.OrderItemRequest> items, CardInfo cardInfo, Long couponId) {
        return new OrderCreateCommand(userId, items, cardInfo, couponId);
    }

    public boolean isCardPayment() {
        return cardInfo != null;
    }

    public boolean hasCoupon() {
        return couponId != null;
    }
}
