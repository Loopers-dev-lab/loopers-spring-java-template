package com.loopers.domain.payment;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING("결제 대기"),
    COMPLETED("결제 완료"),
    FAILED("결제 실패"),
    CANCELED("결제 취소"),
    REFUNDED("환불 완료");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }
}

