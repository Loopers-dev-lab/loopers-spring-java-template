package com.loopers.domain.payment;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    LIMIT_EXCEEDED,
    INVALID_CARD,
    PG_TIMEOUT,
    CALLBACK_TIMEOUT
}
