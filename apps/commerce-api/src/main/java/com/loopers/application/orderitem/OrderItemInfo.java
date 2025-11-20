package com.loopers.application.orderitem;

import com.loopers.domain.orderitem.OrderItem;

import java.math.BigDecimal;

public record OrderItemInfo(Long id, Long orderId, Long productId, int quantity, BigDecimal orderPrice) {
    public static OrderItemInfo from(OrderItem orderItem, BigDecimal productPrice) {
        return new OrderItemInfo(
                orderItem.getId(),
                orderItem.getOrderId(),
                orderItem.getProductId(),
                orderItem.getQuantity(),
                productPrice.multiply(BigDecimal.valueOf(orderItem.getQuantity()))
        );
    }
}
