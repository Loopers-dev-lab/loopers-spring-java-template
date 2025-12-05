package com.loopers.domain.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentApproveResponse(
        @JsonProperty("success")
        Boolean success,

        @JsonProperty("data")
        TransactionData data,

        @JsonProperty("error")
        ErrorData error
) {
    public record TransactionData(
            String transactionKey,
            TransactionStatus status,
            String reason
    ) {
    }

    public record ErrorData(
            String type,
            String message
    ) {
    }

    public enum TransactionStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success) && data != null;
    }

    public String getTransactionKey() {
        return data != null ? data.transactionKey() : null;
    }

    public TransactionStatus getStatus() {
        return data != null ? data.status() : null;
    }

    public String getReasonOrError() {
        if (data != null && data.reason() != null) {
            return data.reason();
        }
        if (error != null && error.message() != null) {
            return error.message();
        }
        return null;
    }
}
/// **
// * pg-simulator 응답을 도메인 응답으로 변환
// */
//public static PaymentApproveResponse from(PgPaymentResponse pgResponse) {
//    if (pgResponse == null || !pgResponse.isSuccess()) {
//        String errorMsg = pgResponse != null ? pgResponse.getReasonOrError() : "Unknown error";
//        return new PaymentApproveResponse(null, "FAILED", errorMsg);
//    }
//
//    return new PaymentApproveResponse(
//            pgResponse.getTransactionKey(),
//            pgResponse.getStatus() != null ? pgResponse.getStatus().name() : "UNKNOWN",
//            pgResponse.getReasonOrError()
//    );
//}

