package com.loopers.domain.order;

public enum PaymentStatus {
    PENDING("대기중"),
    SUCCESS("성공"),
    FAILED("실패");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
