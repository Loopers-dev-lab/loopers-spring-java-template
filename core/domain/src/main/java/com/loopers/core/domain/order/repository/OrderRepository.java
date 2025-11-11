package com.loopers.core.domain.order.repository;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.common.vo.PageNo;
import com.loopers.core.domain.common.vo.PageSize;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.user.vo.UserId;

public interface OrderRepository {

    Order save(Order order);

    OrderListView findListWithCondition(UserId userId, OrderSort createdAtSort, PageNo pageNo, PageSize pageSize);
}
