package com.loopers.infrastructure.feign;

public class PgApiException extends RuntimeException {
  public PgApiException(String message) {
    super(message);
  }
}