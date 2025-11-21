package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Optional<Order> findByIdAndUserId(Long id, Long userId) {
        return orderJpaRepository.findByIdAndUserId(id, userId);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public List<Order> findAllByUserId(Long userId) {
        return orderJpaRepository.findAllByUserId(userId);
    }
}
