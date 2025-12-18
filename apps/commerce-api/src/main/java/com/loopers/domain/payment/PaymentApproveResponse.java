package com.loopers.domain.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentApproveResponse(
        @JsonProperty("meta")
        Meta meta,

        @JsonProperty("data")
        TransactionData data
) {

    public record Meta(
            Result result,
            String errorCode,
            String message
    ) {
        public enum Result {
            SUCCESS,
            FAIL
        }
    }

    public record TransactionData(
            String transactionKey,
            TransactionStatus status,
            String reason
    ) {
    }

    public enum TransactionStatus {
        PENDING,
        SUCCESS,
        FAILED
    }
}

