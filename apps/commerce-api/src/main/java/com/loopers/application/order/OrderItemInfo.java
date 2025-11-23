package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

public record OrderItemInfo(
        Long productId,
        int quantity
){
    public static OrderItemInfo from(OrderItem item) {
        return new OrderItemInfo(
                item.getProductId(),
                item.getQuantity()
        );
    }

    public static OrderItemInfo of(Long productId, int quantity) {

        return new OrderItemInfo(productId, quantity);
    }
}
