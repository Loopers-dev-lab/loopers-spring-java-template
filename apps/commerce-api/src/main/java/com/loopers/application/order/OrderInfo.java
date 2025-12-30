package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

public record OrderInfo(
        Long orderId,
        String userId,
        Long totalAmount,
        OrderStatus status,
        List<OrderItemInfo> items
) {
    public static OrderInfo from(Order order) {
        List<OrderItemInfo> itemInfos = order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .toList();

        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                itemInfos
        );
    }
}
