package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByUserId(Long userId);
}

