package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record OrderInfo(
    Long id,
    String userId,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderItemInfo> orderItems,
    ZonedDateTime createdAt,
    LocalDateTime canceledAt
) {

    public static OrderInfo from(Order order) {
        return OrderInfo.builder()
            .id(order.getId())
            .userId(order.getUserId())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .orderItems(order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .toList())
            .createdAt(order.getCreatedAt())
            .canceledAt(order.getCanceledAt())
            .build();
    }
}
