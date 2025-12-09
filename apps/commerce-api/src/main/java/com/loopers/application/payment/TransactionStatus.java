package com.loopers.application.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {
  PENDING("결제 대기"),
  SUCCESS("결제 승인"),
  FAILED("결제 실패");

  private final String description;
  
}
