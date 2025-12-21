package com.loopers.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
  PRODUCT_LIKED("product_liked", "상품 좋아요"),
  PRODUCT_UNLIKED("product_unliked", "상품 좋아요 취소"),
  PRODUCT_OUT_OF_STOCK("product_out_of_stock", "상품 재고 소진"),
  PRODUCT_SOLD("product_sold", "상품 판매"),
  PRODUCT_VIEWED("product_viewed", "상품 조회"),
  ;

  private final String code;
  private final String description;

  public boolean matches(String code) {
    return this.code.equals(code);
  }
}