package com.loopers.application.order.event;

import com.loopers.domain.order.OrderItem;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record OrderCreatedEvent(
    String eventId,
    Long orderId,
    Long userId,
    List<OrderItemInfo> items,
    long finalAmount,
    PaymentType paymentType,
    String cardType,
    String cardNo,
    Long couponId
) {

  public record OrderItemInfo(Long productId, String productName, int quantity, long price, int remainStock) {

  }

  public static OrderCreatedEvent of(
      Long orderId,
      User user,
      List<OrderItem> orderItems,
      List<Product> products,
      long finalAmount,
      PaymentType paymentType,
      String cardType,
      String cardNo,
      Long couponId
  ) {

    Map<Long, Product> productMap = products.stream()
        .collect(Collectors.toMap(Product::getId, p -> p));

    List<OrderItemInfo> itemInfos = orderItems.stream()
        .map(item -> {
          Product product = productMap.get(item.getProductId());
          return new OrderItemInfo(
              item.getProductId(),
              product != null ? product.getName() : "Unknown",
              item.getQuantity(),
              product != null ? product.getPrice().getValue() : 0,
              product != null ? product.getStock() : 0
          );
        }).toList();

    return new OrderCreatedEvent(
        UUID.randomUUID().toString(),
        orderId,
        user.getId(),
        itemInfos,
        finalAmount,
        paymentType,
        cardType,
        cardNo,
        couponId
    );
  }
}
