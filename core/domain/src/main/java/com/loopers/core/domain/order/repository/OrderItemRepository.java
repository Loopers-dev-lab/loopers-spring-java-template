package com.loopers.core.domain.order.repository;

import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.vo.OrderId;

import java.util.List;

public interface OrderItemRepository {

    OrderItem save(OrderItem orderItem);

    List<OrderItem> findAllByOrderId(OrderId orderId);
}
