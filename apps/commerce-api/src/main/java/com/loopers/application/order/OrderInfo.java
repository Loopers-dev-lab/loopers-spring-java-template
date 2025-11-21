package com.loopers.application.order;

import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import java.util.List;

public record OrderInfo(
    Long orderId,
    Long userId,
    List<OrderItemInfo> items,
    long totalAmount
) {

  public static OrderInfo from(Order order) {
    return new OrderInfo(
        order.getId(),
        order.getUserId(),
        order.getOrderItems().stream().map(OrderItemInfo::from).toList(),
        order.getTotalAmount().getValue()
    );
  }

  public record OrderItemInfo(Long productId, int quantity, Money price) {

    public static OrderItemInfo from(OrderItem item) {
      return new OrderItemInfo(
          item.getProductId(),
          item.getQuantity(),
          item.getPrice()
      );
    }
  }
}
