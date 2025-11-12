package com.loopers.domain.order.orderitem;

import com.loopers.domain.quantity.Quantity;

public record OrderItemCommand(
    Long productId,
    String productName,
    Quantity quantity,
    OrderPrice orderPrice
) {

  public static OrderItemCommand of(
      Long productId,
      String productName,
      Quantity quantity,
      OrderPrice orderPrice
  ) {
    return new OrderItemCommand(productId, productName, quantity, orderPrice);
  }

  public Integer getQuantityValue() {
    return quantity.getValue();
  }

  public Long getOrderPriceValue() {
    return orderPrice.getValue();
  }
}
