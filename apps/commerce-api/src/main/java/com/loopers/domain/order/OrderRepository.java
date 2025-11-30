package com.loopers.domain.order;


import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findByIdAndUser(Long orderId, Long userId);
    List<Order> findAllByUser(Long userId);
}
