package com.loopers.application.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PgPaymentListResponse(
    @JsonProperty("payments")
    List<PgPaymentInfoResponse> payments
) {
}