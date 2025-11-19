package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;

import java.util.List;

public record OrderInfo(
        Long orderId,
        String userId,
        List<OrderItemInfo> items,
        long totalAmount,
        String status
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getOrderItems().stream()
                        .map(OrderItemInfo::from)
                        .toList(),
                order.getTotalAmount(),
                order.getStatus().name()
        );
    }

    public record OrderItemInfo(
            Long productId,
            String productName,
            Long quantity,
            long price
    ) {
        public static OrderItemInfo from(OrderItem item) {
            return new OrderItemInfo(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getPrice()
            );
        }
    }
}
