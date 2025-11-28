package com.loopers.application.order;

import com.loopers.application.orderitem.OrderItemInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderInfo (
        Long orderId,
        OrderStatus status,
        BigDecimal totalPrice,
        String userId,
        List<OrderItemInfo> orderItems,
        LocalDateTime createdAt
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getStatus(),
                order.getTotalPrice().getAmount(),
                order.getUser().getUserId(),
                order.getOrderItems().stream().map(OrderItemInfo::from).toList(),
                order.getCreatedAt().toLocalDateTime()
        );
    }
}
