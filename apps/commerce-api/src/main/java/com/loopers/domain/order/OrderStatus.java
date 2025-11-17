package com.loopers.domain.order;


public enum OrderStatus {
  PENDING("주문 생성 완료"),
  COMPLETED("결제 완료"),
  PAYMENT_FAILED("결제 실패");

  private final String description;

  OrderStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
