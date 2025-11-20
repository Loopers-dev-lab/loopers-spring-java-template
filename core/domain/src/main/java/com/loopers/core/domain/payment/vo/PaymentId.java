package com.loopers.core.domain.payment.vo;

public record PaymentId(String value) {

    public static PaymentId empty() {
        return new PaymentId(null);
    }
}
