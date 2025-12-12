package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record OrderCreateCommand(
    Long userId,
    List<OrderItem> orderItems,
    Long pointUsedAmount,
    Long discountAmount,
    Long pgAmount,
    Long totalAmount,
    Long couponId,
    LocalDateTime orderedAt
) {

  public OrderCreateCommand(
      Long userId,
      List<OrderItem> orderItems,
      Long pointUsedAmount,
      Long discountAmount,
      Long pgAmount,
      Long totalAmount,
      Long couponId,
      LocalDateTime orderedAt
  ) {
    this.userId = Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    this.orderItems = Objects.requireNonNull(orderItems, "orderItems는 null일 수 없습니다.");
    this.pointUsedAmount = Objects.requireNonNull(pointUsedAmount, "pointUsedAmount는 null일 수 없습니다.");
    this.discountAmount = Objects.requireNonNull(discountAmount, "discountAmount는 null일 수 없습니다.");
    this.pgAmount = Objects.requireNonNull(pgAmount, "pgAmount는 null일 수 없습니다.");
    this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount는 null일 수 없습니다.");
    this.couponId = couponId;
    this.orderedAt = Objects.requireNonNull(orderedAt, "orderedAt는 null일 수 없습니다.");
  }

  public static OrderCreateCommand of(
      Long userId,
      OrderPreparation preparation,
      OrderPaymentCalculation payment,
      Long couponId,
      LocalDateTime orderedAt
  ) {
    return new OrderCreateCommand(
        userId,
        preparation.orderItems().getItems(),
        payment.pointAmount().getValue(),
        payment.discountAmount().getValue(),
        payment.pgAmount().getValue(),
        preparation.totalAmount(),
        couponId,
        orderedAt
    );
  }
}
