package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class OrderRepositoryImpl implements OrderRepository{
    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public List<Order> findAllByUserId(String userId) {
        return orderJpaRepository.findAllByUserId(userId);
    }
}
