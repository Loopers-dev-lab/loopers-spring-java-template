package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import java.util.List;
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
}
