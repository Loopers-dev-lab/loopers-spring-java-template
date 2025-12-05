package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Arrays;

public enum PaymentStatus {
  REQUESTED("REQUESTED", "PG 요청 전"),
  PENDING("PENDING", "PG 처리 중"),
  SUCCESS("SUCCESS", "결제 성공"),
  FAILED("FAILED", "결제 실패"),
  REQUEST_FAILED("REQUEST_FAILED", "PG 요청 실패");

  private final String code;
  private final String description;

  PaymentStatus(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public boolean isSameCode(String code) {
    return this.code.equals(code);
  }

  public static PaymentStatus from(String code) {
    return Arrays.stream(values())
        .filter(s -> s.code.equals(code))
        .findFirst()
        .orElseThrow(() -> new CoreException(ErrorType.INVALID_PAYMENT_STATUS));
  }

  public boolean isCompleted() {
    return this == SUCCESS || this == FAILED || this == REQUEST_FAILED;
  }

  public boolean isSuccess() {
    return this == SUCCESS;
  }
}
