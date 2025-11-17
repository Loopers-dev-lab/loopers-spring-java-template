package com.loopers.domain.order;

import lombok.Getter;

@Getter
public enum OrderStatus {
    CREATED("주문 생성"),
    PAID("결제 완료"),
    CANCELED("주문 취소"),
    COMPLETED("주문 완료");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

}
