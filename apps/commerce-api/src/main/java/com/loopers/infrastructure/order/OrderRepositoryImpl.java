package com.loopers.infrastructure.order;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;

import lombok.RequiredArgsConstructor;

/**
 * 주문 Repository 구현체
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public OrderEntity save(OrderEntity order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<OrderEntity> findByIdAndUserId(Long id, Long userId) {
        return orderJpaRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
    }

    @Override
    public Page<OrderEntity> findByUserId(Long userId, Pageable pageable) {
        return orderJpaRepository.findByUserIdAndDeletedAtIsNull(userId, pageable);
    }

    @Override
    public Page<OrderEntity> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable) {
        return orderJpaRepository.findByUserIdAndStatusAndDeletedAtIsNull(userId, status, pageable);
    }
}
