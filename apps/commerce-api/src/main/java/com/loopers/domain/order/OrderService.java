package com.loopers.domain.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public Optional<Order> findOrder(Long id, Long userId) {
        return orderRepository.findByIdAndUserId(id, userId);
    }

    public Optional<Order> findOrderById(Long id) {
        return orderRepository.findById(id);
    }
    public Optional<Order> findOrderWithItems(Long userId, Long id) {
        return orderRepository.findOrderWithItems(userId, id);
    }

    public List<Order> findAll(Long userId) {
        return orderRepository.findAllByUserId(userId);
    }

    public List<Order> findPendingOrders(Long duration) {
        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(duration);
        return orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, threshold);
    }

    @Transactional
    public void markFailed(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.changeToFailed();
    }

    @Transactional
    public void markPending(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.changeToPending();
    }

    @Transactional
    public void markPaid(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.changeToPaid();
    }
}
