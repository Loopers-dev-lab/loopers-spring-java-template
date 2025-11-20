package com.loopers.domain.order;

import java.util.List;

public interface OrderRepository {
    Order save(Order order);

    List<OrderItem> saveOrderItems(List<OrderItem> orderItems);
}
