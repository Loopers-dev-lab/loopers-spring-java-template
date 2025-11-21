package com.loopers.application.order;

import com.loopers.interfaces.api.order.OrderCreateV1Dto;

import java.util.List;

public record CreateOrderCommand(Long userId, List<OrderItemCommand> orderItemCommands) {
  public record OrderItemCommand(Long productId, Long quantity) {

  }

  public static CreateOrderCommand from(Long userId, OrderCreateV1Dto.OrderRequest request) {
    return new CreateOrderCommand(
        userId,
        request.items().stream()
            .map(item -> new OrderItemCommand(item.productId(), item.quantity()))
            .toList()
    );
  }
}
