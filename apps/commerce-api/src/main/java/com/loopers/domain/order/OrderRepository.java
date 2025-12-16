package com.loopers.domain.order;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(Long id);

    Optional<Order> findOrderWithDetailsById(Long id);
}
