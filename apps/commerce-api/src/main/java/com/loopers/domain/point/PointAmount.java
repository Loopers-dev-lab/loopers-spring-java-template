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
    this.amount = amount;
  }

  public static PointAmount zero() {
    return new PointAmount(0L);
  }

}
