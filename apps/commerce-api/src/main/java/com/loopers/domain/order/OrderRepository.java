package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    
    List<Order> findOrdersByUserId(Long userId);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    void saveOrder(Order order);
}
