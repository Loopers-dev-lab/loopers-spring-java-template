package com.loopers.domain.order.orderitem;

import com.loopers.domain.quantity.Quantity;

public record OrderItemCommand(
    Long productId,
    Quantity quantity
) {

  public static OrderItemCommand of(Long productId, Quantity quantity) {
    return new OrderItemCommand(productId, quantity);
  }
}
