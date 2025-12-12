package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    
    List<Order> findOrdersByUserId(Long userId);

    Optional<Order> findOrderByIdAndUserId(Long orderId, Long userId);

    Optional<Order> findByOrderKey(String orderKey);

    void saveOrder(Order order);

}
