package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
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
    public List<Order> findPendingPaymentOrdersBefore(ZonedDateTime before) {
        return orderJpaRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, before);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return orderJpaRepository.findById(orderId);
    }

    @Override
    public Page<Order> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable) {
        return orderJpaRepository.findByUserIdAndDeletedAtIsNull(userId, pageable);
    }

    @Override
    public Optional<Order> findByOrderId(String orderId) {
        return orderJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Order> findByIdAndUserIdForUpdate(Long orderId, Long userId) {
        return orderJpaRepository.findByIdAndUserIdForUpdate(orderId, userId);
    }
}
