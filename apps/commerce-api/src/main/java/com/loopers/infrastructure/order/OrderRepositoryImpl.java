package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public List<Order> findOrdersByUserId(Long userId) {
        return orderJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Order> findOrderByIdAndUserId(Long orderId, Long userId) {
        return orderJpaRepository.findByIdAndUserId(orderId, userId);
    }

    @Override
    public void saveOrder(Order order) {
        orderJpaRepository.save(order);
    }

}
