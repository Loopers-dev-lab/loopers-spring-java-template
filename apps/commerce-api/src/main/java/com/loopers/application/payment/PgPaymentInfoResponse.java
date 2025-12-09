package com.loopers.application.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PgPaymentInfoResponse(
    @JsonProperty("transactionKey")
    String transactionKey,
    @JsonProperty("orderId")
    String orderId,
    @JsonProperty("amount")
    Long amount,
    @JsonProperty("status")
    String status
) {
}