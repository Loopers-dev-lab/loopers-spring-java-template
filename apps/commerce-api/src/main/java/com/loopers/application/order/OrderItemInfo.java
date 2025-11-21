package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

import java.util.List;

public record OrderItemInfo(
        Long productId,
        String productName,
        Integer quantity,
        Integer totalPrice
) {
    public static OrderItemInfo from(OrderItem orderItem) {
        return new OrderItemInfo(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getTotalPrice()
        );
    }

    public static List<OrderItemInfo> fromList(List<OrderItem> items) {
        return items.stream()
                .map(OrderItemInfo::from)
                .toList();
    }
}
