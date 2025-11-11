package com.loopers.core.infra.database.mysql.order.impl;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.common.vo.PageNo;
import com.loopers.core.domain.common.vo.PageSize;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.user.vo.UserId;
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

    @Override
    public OrderListView findListWithCondition(
            UserId userId,
            OrderSort createdAtSort,
            PageNo pageNo,
            PageSize pageSize
    ) {
        return null;
    }
}
