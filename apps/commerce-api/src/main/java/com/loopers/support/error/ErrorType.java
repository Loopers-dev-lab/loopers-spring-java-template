package com.loopers.support.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
  /**
   * 범용 에러
   */
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했습니다."),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "존재하지 않는 요청입니다."),
  CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 존재하는 리소스입니다."),

  INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOCK", "요청하신 상품의 재고가 부족합니다."),
  INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINT", "사용 가능한 포인트가 부족합니다.");
  private final HttpStatus status;
  private final String code;
  private final String message;
}
