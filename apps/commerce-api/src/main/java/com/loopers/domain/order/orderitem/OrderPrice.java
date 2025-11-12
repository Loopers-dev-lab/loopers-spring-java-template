package com.loopers.domain.order.orderitem;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class OrderPrice {

  private Long value;

  private OrderPrice(Long value) {
    if (value == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_PRICE_VALUE);
    }
    if (value < 0) {
      throw new CoreException(ErrorType.NEGATIVE_ORDER_PRICE_VALUE);
    }
    this.value = value;
  }

  public static OrderPrice of(Long value) {
    return new OrderPrice(value);
  }

  public Long getValue() {
    return value;
  }
}
