package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

public record OrderItemInfo(Long id, Integer quantity, Integer productPrice, Long productId, Long orderId) {
    public static OrderItemInfo from(final OrderItem model) {
        return new OrderItemInfo(
                model.getId(),
                model.getQuantity(),
                model.getProductPrice(),
                model.getProductId(),
                model.getOrderId()
        );
    }
}
