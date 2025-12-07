package com.loopers.core.domain.order.vo;

import java.util.UUID;

public record OrderKey(String value) {

    public static OrderKey generate() {
        return new OrderKey(
                UUID.randomUUID().toString()
                        .replaceAll("-", "")
                        .substring(0, 12)
                        .toLowerCase()
        );
    }
}
