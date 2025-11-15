package com.loopers.domain.order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
  PENDING("결제 대기", 10),
  PAID("결제 완료", 20),
  PREPARING("상품 준비 중", 30),
  SHIPPED("배송 중", 40),
  DELIVERED("배송 완료", 50),
  CANCELLED("주문 취소", 90),
  REFUNDED("환불 완료", 100);

  private final String description;
  @Getter(AccessLevel.NONE)
  private final int sequence;

  public int compare(OrderStatus other) {
    return Integer.compare(this.sequence, other.sequence);
  }
}
