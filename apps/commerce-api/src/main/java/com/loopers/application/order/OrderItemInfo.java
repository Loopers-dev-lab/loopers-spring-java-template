package com.loopers.application.order;

import com.loopers.domain.common.Money;
import com.loopers.domain.order.OrderItemModel;
import com.loopers.application.order.OrderItemInfo;

public record OrderItemInfo(Long productId, Integer quantity, Money orderPrice) {
    public static OrderItemInfo from(OrderItemModel item) {
        return new OrderItemInfo(
            item.getProduct().getId(),
            item.getQuantity().quantity(),
            item.getOrderPrice()
        );
    }
    }
