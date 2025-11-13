package com.loopers.domain.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(String userId, List<OrderItem> orderItems, int totalAmount) {
        // 주문 생성 (PENDING)
        Order order = Order.create(userId, orderItems, totalAmount);

        // 주문 확정 (CONFIRMED)
        order.confirm();

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public record OrderItemRequest(Long productId, Long quantity) {
    }
}
