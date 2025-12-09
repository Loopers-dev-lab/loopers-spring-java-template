package com.loopers.domain.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
  PENDING("결제 대기", 10),
  APPROVED("결제 승인", 20),
  FAILED("결제 실패", 30);

  private final String description;
  @Getter(AccessLevel.NONE)
  private final int sequence;

  public int compare(PaymentStatus other) {
    return Integer.compare(this.sequence, other.sequence);
  }
}
