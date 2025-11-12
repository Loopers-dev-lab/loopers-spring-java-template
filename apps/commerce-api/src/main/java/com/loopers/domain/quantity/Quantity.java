package com.loopers.domain.quantity;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@Getter
public class Quantity {

  private Integer value;

  private Quantity(Integer value) {
    if (value == null) {
      throw new CoreException(ErrorType.INVALID_QUANTITY_VALUE);
    }
    if (value < 0) {
      throw new CoreException(ErrorType.INVALID_QUANTITY_RANGE);
    }
    this.value = value;
  }

  public static Quantity of(Integer value) {
    return new Quantity(value);
  }
}
