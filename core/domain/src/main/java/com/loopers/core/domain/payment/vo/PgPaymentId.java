package com.loopers.core.domain.payment.vo;

public record PgPaymentId(String value) {

    public static PgPaymentId empty() {
        return new PgPaymentId(null);
    }
}
