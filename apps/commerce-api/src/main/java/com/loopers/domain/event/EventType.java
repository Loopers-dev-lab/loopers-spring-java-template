package com.loopers.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
  // catalog-events
  PRODUCT_LIKED("product_liked", "상품 좋아요", "catalog-events"),
  PRODUCT_UNLIKED("product_unliked", "상품 좋아요 취소", "catalog-events"),
  PRODUCT_OUT_OF_STOCK("product_out_of_stock", "상품 재고 소진", "catalog-events"),
  PRODUCT_SOLD("product_sold", "상품 판매", "catalog-events"),
  PRODUCT_VIEWED("product_viewed", "상품 조회", "catalog-events"),

  // order-events
  ORDER_CREATED("order_created", "주문 생성", "order-events"),
  ORDER_COMPLETED("order_completed", "주문 완료", "order-events"),

  // 미발행 이벤트 (내부 처리용)
  POINT_PAYMENT_COMPLETED("point_payment_completed", "포인트 결제 완료", null),
  PAYMENT_SUCCEEDED("payment_succeeded", "결제 성공", null),
  PAYMENT_FAILED("payment_failed", "결제 실패", null),
  ;

  private final String code;
  private final String description;
  private final String topic;
}
