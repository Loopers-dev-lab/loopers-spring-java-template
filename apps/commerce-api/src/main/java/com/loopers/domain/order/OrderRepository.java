package com.loopers.domain.order;

import com.loopers.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findByIdAndUser(Long orderId, User user);
    List<Order> findAllByUser(User user);
}
