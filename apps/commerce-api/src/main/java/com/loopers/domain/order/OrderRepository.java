package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Order save(Order order);

    List<Order> findAllByUserId(Long userId);
}
