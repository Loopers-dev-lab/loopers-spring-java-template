package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record OrderInfo(
        Long id,
        Long userId,
        OrderStatus orderStatus,
        BigDecimal totalPrice,
        ZonedDateTime createdAt
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getOrderStatus(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }
}
