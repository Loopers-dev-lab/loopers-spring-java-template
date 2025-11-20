package com.loopers.domain.order;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(Long userId) {
        Order order = Order.create(userId);
        return orderRepository.save(order);
    }

    @Transactional
    public List<OrderItem> createOrderItems(List<OrderItem> orderItems) {
        return orderRepository.saveOrderItems(orderItems);
    }
}
