package com.loopers.core.domain.order;

import lombok.Getter;

import java.util.List;

@Getter
public class OrderDetail {

    private final Order order;

    private final List<OrderItem> orderItems;

    public OrderDetail(Order order, List<OrderItem> orderItems) {
        this.order = order;
        this.orderItems = orderItems;
    }
}
