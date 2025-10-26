package com.loopers.core.domain.user.vo;

public record UserId(String value) {

    public static UserId empty() {
        return new UserId(null);
    }
}
