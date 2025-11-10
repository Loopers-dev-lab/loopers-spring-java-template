package com.loopers.core.infra.database.mysql.order.impl;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.infra.database.mysql.order.entity.OrderEntity;
import com.loopers.core.infra.database.mysql.order.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository repository;

    @Override
    public Order save(Order order) {
        return repository.save(OrderEntity.from(order)).to();
    }
}
