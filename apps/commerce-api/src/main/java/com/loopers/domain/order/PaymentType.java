package com.loopers.domain.order;

/**
 * 결제 수단 유형
 */
public enum PaymentType {
    POINT_ONLY("포인트만 사용"),
    CARD_ONLY("카드만 사용"),
    MIXED("포인트와 카드 혼합");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
