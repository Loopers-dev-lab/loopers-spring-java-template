package com.loopers.application.order;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.OrderItem;

public record OrderItemInfo(
        Long productId,
        int quantity,
        Money totalPrice
){
    public static OrderItemInfo from(OrderItem item) {
        return new OrderItemInfo(
                item.getProductId(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }

}
