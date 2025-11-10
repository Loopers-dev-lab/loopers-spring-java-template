package com.loopers.core.domain.order.repository;

import com.loopers.core.domain.order.Order;

public interface OrderRepository {

    Order save(Order order);
}
