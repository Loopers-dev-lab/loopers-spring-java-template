package com.loopers.application.order;

import com.loopers.domain.order.Order;

import java.util.List;

public record OrderInfo(
        Long orderId,
        Long userId,
        Integer totalPrice,
        List<OrderItemInfo> items
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice().amount(),
                OrderItemInfo.fromList(order.getOrderItems())
        );
    }
}
