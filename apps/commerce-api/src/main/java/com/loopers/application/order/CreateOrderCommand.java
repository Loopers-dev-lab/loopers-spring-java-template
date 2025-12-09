package com.loopers.application.order;

import com.loopers.interfaces.api.order.OrderCreateV1Dto;

import java.util.List;

public record CreateOrderCommand(Long userId, List<OrderItemRequest> orderItemRequests, Long couponId) {
  public record OrderItemRequest(Long productId, Long quantity) {

  }

  public static CreateOrderCommand from(Long userId, OrderCreateV1Dto.OrderRequest request) {
    return new CreateOrderCommand(
        userId,
        request.items().stream()
            .map(item -> new OrderItemRequest(item.productId(), item.quantity()))
            .toList(),
        request.couponId()
    );
  }
}
