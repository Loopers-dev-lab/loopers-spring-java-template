package com.loopers.domain.order;

import com.loopers.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByUser(User user);
}

