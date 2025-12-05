package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;
    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public Order save(final Order order) {
        return orderJpaRepository.saveAndFlush(order);
    }

    @Override
    public List<OrderItem> saveOrderItems(final List<OrderItem> orderItems) {
        return orderItemJpaRepository.saveAllAndFlush(orderItems);
    }

    @Override
    public Optional<Order> findByIdAndUserIdAndOrderStatus(final Long orderId, final Long userId, final OrderStatus orderStatus) {
        return orderJpaRepository.findByIdAndUserIdAndOrderStatus(orderId, userId, orderStatus);
    }

    @Override
    public List<OrderItem> findOrderItemsByOrderId(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }
}
