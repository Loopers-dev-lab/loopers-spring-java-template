package com.loopers.core.domain.order.repository;

import com.loopers.core.domain.order.OrderItem;

public interface OrderItemRepository {

    OrderItem save(OrderItem orderItem);
}
