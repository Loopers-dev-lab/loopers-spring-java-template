package com.loopers.infrastructure.pg;

public class PgRequestFailedException extends RuntimeException {

  public PgRequestFailedException(String message) {
    super(message);
  }

  public PgRequestFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
