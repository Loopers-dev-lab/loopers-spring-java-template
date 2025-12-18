package com.loopers.domain.product;

import java.util.List;

public record StockDecreaseResult(
    boolean succeeded,
    List<Long> insufficientProductIds
) {

  public static StockDecreaseResult success() {
    return new StockDecreaseResult(true, List.of());
  }

  public static StockDecreaseResult failure(List<Long> insufficientProductIds) {
    return new StockDecreaseResult(false, insufficientProductIds);
  }

  public boolean isFailure() {
    return !succeeded;
  }
}
