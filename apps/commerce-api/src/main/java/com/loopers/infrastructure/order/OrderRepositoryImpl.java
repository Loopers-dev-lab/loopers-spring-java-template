package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Optional<Order> findByIdAndUserId(Long id, Long userId) {
        return orderJpaRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Page<Order> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable) {
        return orderJpaRepository.findByUserIdAndDeletedAtIsNull(userId, pageable);
    }
}
