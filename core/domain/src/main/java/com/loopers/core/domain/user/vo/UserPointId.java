package com.loopers.core.domain.user.vo;

public record UserPointId(String value) {

    public static UserPointId empty() {
        return new UserPointId(null);
    }
}
