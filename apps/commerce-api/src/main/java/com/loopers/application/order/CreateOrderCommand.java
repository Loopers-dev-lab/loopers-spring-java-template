package com.loopers.application.order;

import com.loopers.interfaces.api.order.OrderCreateV1Dto;

import java.util.Map;
import java.util.stream.Collectors;

public record CreateOrderCommand(Long userId, Map<Long, Long> orderItemInfo) {
  public record ItemCommand(Long productId, Map<Long, Long> quantity) {
  }

  public static CreateOrderCommand from(Long userId, OrderCreateV1Dto.OrderRequest request) {
    return new CreateOrderCommand(
        userId,
        request.items().stream()
            .collect(Collectors.toMap(
                OrderCreateV1Dto.OrderItemRequest::productId,
                OrderCreateV1Dto.OrderItemRequest::quantity,
                Long::sum
            )));
  }
}
