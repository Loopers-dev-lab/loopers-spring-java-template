package com.loopers.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {

  ORDER_CREATED("order_created"),
  ORDER_COMPLETED("order_completed"),
  POINT_PAYMENT_COMPLETED("point_payment_completed"),
  PRODUCT_LIKED("product_liked"),
  PRODUCT_UNLIKED("product_unliked"),
  PAYMENT_SUCCEEDED("payment_succeeded"),
  PAYMENT_FAILED("payment_failed"),
  ;

  private final String code;
}
