package com.loopers.domain.order;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Long totalAmount,
        Long couponId,
        List<OrderItemInfo> items,
        ZonedDateTime createdAt
) {
    public static OrderCreatedEvent from(Order order) {
        List<OrderItemInfo> itemInfos = order.getOrderItems().stream()
                .map(item -> new OrderItemInfo(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPriceValue()
                ))
                .toList();

        return new OrderCreatedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getTotalAmountValue(),
                order.getCouponId(),
                itemInfos,
                order.getCreatedAt()
        );
    }

    public record OrderItemInfo(Long productId, Integer quantity, Long unitPrice) {
    }
}
