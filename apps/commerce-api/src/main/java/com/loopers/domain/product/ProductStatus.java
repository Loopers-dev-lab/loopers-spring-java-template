package com.loopers.domain.product;

public enum ProductStatus {
    ON_SALE('1'),
    SOLD_OUT('2'),
    STOP_SELLING('Z');

    private final char code;

    ProductStatus(char code) {
        this.code = code;
    }

    public char getCode() { return code; }
}
