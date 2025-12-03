package com.loopers.application.order;

import com.loopers.domain.order.OrderService;

import java.util.List;

/**
 * 주문 생성 커맨드
 */
public class CreateOrderCommand {
    private final String userId;
    private final List<OrderService.OrderItemRequest> orderItems;
    private final int usedPoints;

    public CreateOrderCommand(String userId, List<OrderService.OrderItemRequest> orderItems, int usedPoints) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.usedPoints = usedPoints;
    }

    public String getUserId() {
        return userId;
    }

    public List<OrderService.OrderItemRequest> getOrderItems() {
        return orderItems;
    }

    public int getUsedPoints() {
        return usedPoints;
    }
}
