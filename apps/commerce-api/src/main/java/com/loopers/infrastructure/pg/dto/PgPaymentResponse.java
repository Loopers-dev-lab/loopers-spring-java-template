package com.loopers.infrastructure.pg.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PgPaymentResponse {
    private PgMeta meta;
    private PgPaymentData data;

    public String getTransactionKey() {
        return data != null ? data.transactionKey : null;
    }

    public String getStatus() {
        return data != null ? data.status : null;
    }

    public String getReason() {
        return data != null ? data.reason : null;
    }

    @Getter
    @NoArgsConstructor
    public static class PgMeta {
        private String result;
        private String errorCode;
        private String message;
    }

    @Getter
    @NoArgsConstructor
    public static class PgPaymentData {
        private String transactionKey;
        private String status;
        private String reason;
    }
}
