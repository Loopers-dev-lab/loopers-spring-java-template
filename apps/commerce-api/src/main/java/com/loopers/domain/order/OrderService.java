package com.loopers.domain.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public Optional<Order> findOrder(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId);
    }

    public List<Order> findAll(Long userId) {
        return orderRepository.findAllByUserId(userId);
    }
}
