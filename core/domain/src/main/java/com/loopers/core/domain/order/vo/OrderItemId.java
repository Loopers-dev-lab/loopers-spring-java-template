package com.loopers.core.domain.order.vo;

public record OrderItemId(String value) {

    public static OrderItemId empty() {
        return new OrderItemId(null);
    }
}
