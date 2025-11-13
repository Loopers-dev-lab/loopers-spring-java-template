package com.loopers.domain.brand;

public enum BrandStatus {
    UNREGISTERED('0'),
    REGISTERED('1'),
    DISCONITNUED('Z');

    private final char code;

    BrandStatus(char code) {
        this.code = code;
    }

    public char getCode() { return code; }
}
