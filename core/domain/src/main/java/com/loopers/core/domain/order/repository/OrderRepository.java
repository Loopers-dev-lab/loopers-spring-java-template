package com.loopers.core.domain.order.repository;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.user.vo.UserId;

public interface OrderRepository {

    Order save(Order order);

    Order getById(OrderId orderId);

    OrderListView findListWithCondition(UserId userId, OrderSort createdAtSort, int pageNo, int pageSize);
}
