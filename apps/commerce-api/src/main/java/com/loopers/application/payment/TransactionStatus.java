package com.loopers.application.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {
  PENDING,
  SUCCESS,
  FAILED;

}
