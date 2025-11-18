package com.loopers.domain.order.repository;

import com.loopers.domain.order.Order;

import java.util.Optional;

public interface OrderRepository {
    
    Optional<Order> findById(Long id);
    
    Order save(Order order);
}
