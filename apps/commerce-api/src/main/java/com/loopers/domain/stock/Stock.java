package com.loopers.domain.stock;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Stock {

  private Long value;

  private Stock(Long value) {
    if (value == null) {
      throw new CoreException(ErrorType.INVALID_STOCK_VALUE);
    }
    if (value < 0) {
      throw new CoreException(ErrorType.NEGATIVE_STOCK_VALUE);
    }
    this.value = value;
  }

  public static Stock zero() {
    return new Stock(0L);
  }

  public static Stock of(Long value) {
    return new Stock(value);
  }

  public Stock increase(Long amount) {
    if (amount == null || amount < 0) {
      throw new CoreException(ErrorType.INVALID_STOCK_INCREASE_AMOUNT);
    }
    return Stock.of(this.value + amount);
  }

  public Stock decrease(Long amount) {
    if (amount == null || amount < 0) {
      throw new CoreException(ErrorType.INVALID_STOCK_DECREASE_AMOUNT);
    }
    if (this.value < amount) {
      throw new CoreException(ErrorType.INSUFFICIENT_STOCK);
    }
    return Stock.of(this.value - amount);
  }

  public boolean isAvailable() {
    return this.value > 0;
  }

  public Long getValue() {
    return value;
  }
}
