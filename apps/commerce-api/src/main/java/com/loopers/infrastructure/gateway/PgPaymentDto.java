package com.loopers.infrastructure.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PgPaymentDto {

    public record PgPaymentRequest(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("cardType") String cardType,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("amount") Long amount,
            @JsonProperty("callbackUrl") String callbackUrl
    ) {
    }

    public record PgPaymentResponse(
            @JsonProperty("meta") Meta meta,
            @JsonProperty("data") TransactionData data
    ) {
        public record Meta(
                @JsonProperty("result") String result,
                @JsonProperty("errorCode") String errorCode,
                @JsonProperty("message") String message
        ) {
        }

        public record TransactionData(
                @JsonProperty("transactionKey") String transactionKey,
                @JsonProperty("status") String status,
                @JsonProperty("reason") String reason
        ) {
        }
    }

    public record PgPaymentOrderResponse(
            @JsonProperty("meta") PgPaymentResponse.Meta meta,
            @JsonProperty("data") OrderData data
    ) {
        public record OrderData(
                @JsonProperty("orderId") String orderId,
                @JsonProperty("transactions") List<TransactionData> transactions
        ) {
        }

        public record TransactionData(
                @JsonProperty("transactionKey") String transactionKey,
                @JsonProperty("status") String status,
                @JsonProperty("reason") String reason
        ) {
        }

        public boolean hasTransactionKey() {
            return data.transactions() != null && !data.transactions().isEmpty();
        }
    }
}


