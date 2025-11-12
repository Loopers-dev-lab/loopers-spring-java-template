package com.loopers.core.infra.database.mysql.order.impl;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.infra.database.mysql.order.dto.OrderListProjection;
import com.loopers.core.infra.database.mysql.order.entity.OrderEntity;
import com.loopers.core.infra.database.mysql.order.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository repository;

    @Override
    public Order save(Order order) {
        return repository.save(OrderEntity.from(order)).to();
    }

    @Override
    public Order getById(OrderId orderId) {
        return repository.findById(Long.parseLong(orderId.value()))
                .orElseThrow(() -> NotFoundException.withName("주문")).to();
    }

    @Override
    public OrderListView findListWithCondition(
            UserId userId,
            OrderSort createdAtSort,
            int pageNo,
            int pageSize
    ) {
        Page<OrderListProjection> page = repository.findListByCondition(
                Optional.ofNullable(userId.value())
                        .map(Long::parseLong)
                        .orElse(null),
                createdAtSort,
                PageRequest.of(pageNo, pageSize)
        );

        return new OrderListView(
                page.getContent().stream()
                        .map(OrderListProjection::to)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
