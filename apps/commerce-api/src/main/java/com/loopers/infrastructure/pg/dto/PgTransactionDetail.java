package com.loopers.infrastructure.pg.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PgTransactionDetail {
    private PgPaymentResponse.PgMeta meta;
    private PgTransactionData data;

    public String getTransactionKey() {
        return data != null ? data.transactionKey : null;
    }

    public String getOrderId() {
        return data != null ? data.orderId : null;
    }

    public String getCardType() {
        return data != null ? data.cardType : null;
    }

    public String getCardNo() {
        return data != null ? data.cardNo : null;
    }

    public Long getAmount() {
        return data != null ? data.amount : null;
    }

    public String getStatus() {
        return data != null ? data.status : null;
    }

    public String getReason() {
        return data != null ? data.reason : null;
    }

    @Getter
    @NoArgsConstructor
    public static class PgTransactionData {
        private String transactionKey;
        private String orderId;
        private String cardType;
        private String cardNo;
        private Long amount;
        private String status;
        private String reason;
    }
}
