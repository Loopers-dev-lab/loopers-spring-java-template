package com.loopers.domain.orderitem;


import java.util.List;

public interface OrderItemRepository {
    List<OrderItem> getOrderItemsByOrder(Long orderId);
}
