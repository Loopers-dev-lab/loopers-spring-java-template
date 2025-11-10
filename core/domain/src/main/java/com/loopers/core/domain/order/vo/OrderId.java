package com.loopers.core.domain.order.vo;

public record OrderId(String value) {

    public static OrderId empty() {
        return new OrderId(null);
    }
}
