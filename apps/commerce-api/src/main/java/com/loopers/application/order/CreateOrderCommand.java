package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.interfaces.api.order.OrderCreateV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record CreateOrderCommand(Long userId, List<ItemCommand> orderItemInfo) {
  public record ItemCommand(Long productId, Long quantity) {
  }

  public static CreateOrderCommand from(Long userId, OrderCreateV1Dto.OrderRequest request) {
    return new CreateOrderCommand(
        userId,
        request.items().stream().map(i -> new ItemCommand(i.productId(), i.quantity())).toList());
  }
}
