package com.loopers.core.domain.user.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import static com.loopers.core.domain.error.DomainErrorCode.USER_POINT_BALANCE_NON_NEGATIVE;

public record UserPointBalance(int value) {

    public UserPointBalance(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(USER_POINT_BALANCE_NON_NEGATIVE.getMessage());
        }

        this.value = value;
    }

    public static UserPointBalance init() {
        return new UserPointBalance(0);
    }

    public UserPointBalance add(int point) {
        if (point <= 0) {
            throw new IllegalArgumentException(DomainErrorCode.CANNOT_CHARGE_POINTS_NEGATIVE.getMessage());
        }

        return new UserPointBalance(this.value + point);
    }
}
