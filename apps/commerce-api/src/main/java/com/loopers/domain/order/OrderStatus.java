package com.loopers.domain.order;

public enum OrderStatus {
    PENDING('0'),
    SUCCESS('1'),
    PARTIAL_SUCCESS('2'),
    FAILED('3');

    private final char code;

    OrderStatus(char code) {
        this.code = code;
    }

    public char getCode() { return code; }
}
