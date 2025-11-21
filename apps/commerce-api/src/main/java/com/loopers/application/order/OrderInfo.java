package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.OrderItem;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderInfo(
        Long orderId,
        String userId,
        Long totalAmount,
        OrderStatus status,
        ZonedDateTime paidAt,
        List<OrderItemInfo> items
) {
    public record OrderItemInfo(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPrice,
            Long totalPrice
    ) {
        public static OrderItemInfo from(OrderItem item) {
            return new OrderItemInfo(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPriceValue(),
                    item.calculateAmount().getValue()
            );
        }
    }

    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getUser().getUserIdValue(),
                order.getTotalAmountValue(),
                order.getStatus(),
                order.getPaidAt(),
                order.getOrderItems().stream()
                        .map(OrderItemInfo::from)
                        .toList()
        );
    }
}
