package com.loopers.domain.point;

import com.loopers.domain.money.Money;

public record PointDeductionResult(
    Money deductedAmount,
    Money remainingToPay
) {

  public static PointDeductionResult of(Money deductedAmount, Money remainingToPay) {
    return new PointDeductionResult(deductedAmount, remainingToPay);
  }
}
