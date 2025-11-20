package com.loopers.domain.order;

import lombok.Getter;

@Getter
public enum OrderStatus {
    READY("준비중"),
    SHIPPING("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("배송취소");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

}
