package com.loopers.core.domain.user.vo;

import com.loopers.core.domain.error.DomainErrorCode;
import com.loopers.core.domain.payment.vo.PayAmount;

import java.math.BigDecimal;

public record UserPointBalance(BigDecimal value) {

    private static final String FILED_NAME = "사용자 포인트의 잔액";

    public UserPointBalance(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(DomainErrorCode.negativeMessage(FILED_NAME));
        }

        this.value = value;
    }

    public static UserPointBalance init() {
        return new UserPointBalance(BigDecimal.ZERO);
    }

    public UserPointBalance add(BigDecimal point) {
        if (point.signum() < 0) {
            throw new IllegalArgumentException(DomainErrorCode.CANNOT_CHARGE_POINTS_NEGATIVE.getMessage());
        }

        return new UserPointBalance(this.value.add(point));
    }

    public boolean isPayable(PayAmount payAmount) {
        return this.value.compareTo(payAmount.value()) >= 0;
    }

    public UserPointBalance decrease(PayAmount payAmount) {
        if (this.value.compareTo(payAmount.value()) < 0) {
            throw new IllegalArgumentException(DomainErrorCode.NOT_ENOUGH_USER_POINT_BALANCE.getMessage());
        }

        return new UserPointBalance(this.value.subtract(payAmount.value()));
    }
}
