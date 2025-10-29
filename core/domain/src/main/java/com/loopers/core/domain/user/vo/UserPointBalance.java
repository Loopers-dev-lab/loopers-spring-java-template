package com.loopers.core.domain.user.vo;

import static com.loopers.core.domain.error.DomainErrorCode.USER_POINT_BALANCE_GRATER_THEN_ZERO;

public record UserPointBalance(int value) {

    public UserPointBalance(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(USER_POINT_BALANCE_GRATER_THEN_ZERO.getMessage());
        }

        this.value = value;
    }

    public static UserPointBalance init() {
        return new UserPointBalance(0);
    }
}
