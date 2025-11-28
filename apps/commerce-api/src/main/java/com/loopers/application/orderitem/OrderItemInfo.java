package com.loopers.application.orderitem;

import com.loopers.domain.orderitem.OrderItem;

import java.math.BigDecimal;

public record OrderItemInfo(
        Long orderItemId,
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal totalPrice

) {
    public static OrderItemInfo from(OrderItem orderItem) {
        return new OrderItemInfo(
                orderItem.getId(),
                orderItem.getProduct().getId(),
                orderItem.getProduct().getProductName(),
                orderItem.getPrice().getAmount(),
                orderItem.getQuantity(),
                orderItem.getTotalPrice().getAmount()
        );
    }
}
