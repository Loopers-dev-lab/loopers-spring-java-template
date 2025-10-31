package com.loopers.domain.point;

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
  private Long amount;

  private PointAmount(Long amount) {
    if (amount == null) {
      throw new IllegalArgumentException("포인트 금액은 비어있을 수 없습니다.");
    }
    if (amount < 0) {
      throw new IllegalArgumentException("포인트 금액은 음수일 수 없습니다.");
    }
    this.amount = amount;
  }

  public static PointAmount zero() {
    return new PointAmount(0L);
  }

  public static PointAmount of(Long amount) {
    return new PointAmount(amount);
  }

  public PointAmount add(Long chargeAmount) {
    if (chargeAmount == null || chargeAmount <= 0) {
      throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
    }
    return PointAmount.of(this.amount + chargeAmount);
  }

}
