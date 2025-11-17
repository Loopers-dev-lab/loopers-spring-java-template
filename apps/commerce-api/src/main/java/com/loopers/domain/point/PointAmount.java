package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class PointAmount {

  @Column(name = "amount", nullable = false)
  private Long value;

  private PointAmount(Long value) {
    if (value == null) {
      throw new CoreException(ErrorType.INVALID_POINT_AMOUNT_EMPTY);
    }
    if (value < 0) {
      throw new CoreException(ErrorType.NEGATIVE_POINT_AMOUNT);
    }
    this.value = value;
  }

  public static PointAmount zero() {
    return new PointAmount(0L);
  }

  public static PointAmount of(Long amount) {
    return new PointAmount(amount);
  }

  public PointAmount add(Long chargeAmount) {
    if (chargeAmount == null || chargeAmount <= 0) {
      throw new CoreException(ErrorType.INVALID_CHARGE_AMOUNT);
    }
    return PointAmount.of(this.value + chargeAmount);
  }

  public PointAmount subtract(Long deductAmount) {
    if (deductAmount == null || deductAmount <= 0) {
      throw new CoreException(ErrorType.INVALID_DEDUCT_AMOUNT);
    }
    if (this.value < deductAmount) {
      throw new CoreException(ErrorType.INSUFFICIENT_POINT_BALANCE);
    }
    return PointAmount.of(this.value - deductAmount);
  }

}
