package com.loopers.core.domain.payment.vo;

public record FailedReason(String value) {

    public static FailedReason empty() {
        return new FailedReason(null);
    }
}
