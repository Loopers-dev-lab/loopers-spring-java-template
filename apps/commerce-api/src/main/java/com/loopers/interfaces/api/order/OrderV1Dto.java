package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class OrderV1Dto {

  public record OrderRequest(List<OrderItemRequest> items) {

    public OrderCommand.PlaceOrder toCommand(Long userId) {
      List<OrderCommand.Item> commandItems = items.stream()
          .map(item -> new OrderCommand.Item(item.productId(), item.quantity()))
          .toList();

      return new OrderCommand.PlaceOrder(userId, commandItems);
    }
  }

  public record OrderItemRequest(@NotNull Long productId,
                                 @Positive int quantity
  ) {

  }

  public record OrderResponse(Long orderId,
                              Long userId,
                              long totalPrice
  ) {

    public static OrderResponse from(OrderInfo response) {
      return new OrderResponse(
          response.orderId(),
          response.userId(),
          response.totalAmount()
      );
    }
  }
}
