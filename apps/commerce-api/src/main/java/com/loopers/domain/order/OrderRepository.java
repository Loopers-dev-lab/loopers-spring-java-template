package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    List<OrderItem> saveOrderItems(List<OrderItem> orderItems);

    Optional<Order> findByIdAndUserIdAndOrderStatus(Long orderId, Long userId, OrderStatus orderStatus);

    List<OrderItem> findOrderItemsByOrderId(Long orderId);

    Optional<Order> findById(Long orderId);
}
