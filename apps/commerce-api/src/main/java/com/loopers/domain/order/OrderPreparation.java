package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItems;

public record OrderPreparation(
    OrderItems orderItems,
    Long totalAmount
) {
}
