package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {

  private long amount;

  public Point(long amount) {
    validate(amount);
    this.amount = amount;
  }

  public Point add(long value) {
    if(value <= 0) throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액이 부족합니다.");
    return new Point(this.amount + value);
  }

  public Point subtract(long value) {
    if (this.amount < value) {
      throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액이 부족합니다.");
    }
    return new Point(this.amount - value);
  }

  private void validate(long amount) {
    if (amount < 0) throw new CoreException(ErrorType.BAD_REQUEST);
  }

  public long getAmount() {
    return amount;
  }
}
