package com.loopers.core.domain.payment.vo;

public record CancelledReason(String value) {

    public static CancelledReason empty() {
        return new CancelledReason(null);
    }
}
