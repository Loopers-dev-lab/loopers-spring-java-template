package com.loopers.core.infra.database.mysql.order.impl;

import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.infra.database.mysql.order.entity.OrderItemEntity;
import com.loopers.core.infra.database.mysql.order.repository.OrderItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final OrderItemJpaRepository repository;

    @Override
    public OrderItem save(OrderItem orderItem) {
        return repository.save(OrderItemEntity.from(orderItem)).to();
    }
}
