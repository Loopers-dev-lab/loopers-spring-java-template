package com.loopers.domain.order;

import com.loopers.domain.money.Money;
import java.util.Objects;

public record OrderPaymentCalculation(
    Money discountAmount,
    Money amountAfterDiscount,
    Money pointAmount,
    Money pgAmount
) {

  public OrderPaymentCalculation {
    Objects.requireNonNull(discountAmount, "discountAmount는 null일 수 없습니다.");
    Objects.requireNonNull(amountAfterDiscount, "amountAfterDiscount는 null일 수 없습니다.");
    Objects.requireNonNull(pointAmount, "pointAmount는 null일 수 없습니다.");
    Objects.requireNonNull(pgAmount, "pgAmount는 null일 수 없습니다.");
  }

  public static OrderPaymentCalculation of(
      Money discountAmount,
      Money amountAfterDiscount,
      Money pointAmount,
      Money pgAmount
  ) {
    Objects.requireNonNull(discountAmount, "discountAmount는 null일 수 없습니다.");
    Objects.requireNonNull(amountAfterDiscount, "amountAfterDiscount는 null일 수 없습니다.");
    Objects.requireNonNull(pointAmount, "pointAmount는 null일 수 없습니다.");
    Objects.requireNonNull(pgAmount, "pgAmount는 null일 수 없습니다.");
    return new OrderPaymentCalculation(discountAmount, amountAfterDiscount, pointAmount, pgAmount);
  }
}
