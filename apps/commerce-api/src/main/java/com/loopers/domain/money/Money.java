package com.loopers.domain.money;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Money {

  private Long value;

  private Money(Long value) {
    if (value == null) {
      throw new CoreException(ErrorType.INVALID_MONEY_VALUE);
    }
    if (value < 0) {
      throw new CoreException(ErrorType.NEGATIVE_MONEY_VALUE);
    }
    this.value = value;
  }

  public static Money zero() {
    return new Money(0L);
  }

  public static Money of(Long value) {
    return new Money(value);
  }

  public Long getValue() {
    return value;
  }
}
