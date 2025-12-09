package com.loopers.application.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PgPayResponse(
    @JsonProperty("transactionKey")
    String transactionKey,
    @JsonProperty("status")
    String status,
    @JsonProperty("reason")
    String reason
) {
  public boolean isSuccess() {
    return "PENDING".equals(status);
  }
}
