package com.loopers.core.domain.payment.vo;

public record TransactionKey(String value) {

    public static TransactionKey empty() {
        return new TransactionKey(null);
    }
}
