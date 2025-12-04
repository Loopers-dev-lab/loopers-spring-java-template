package com.loopers.support.util;

/**
 * 멱등성 타입 Enum
 * Redis 키 구분을 위한 타입 정의
 */
public enum IdempotencyType {
    ORDER("order"),
    PAYMENT("payment"),
    COUPON("coupon"),
    POINT("point");
    
    private final String value;
    
    IdempotencyType(String value) {
        this.value = value;
    }
    
    public String getValueString() {
        return value;
    }
}