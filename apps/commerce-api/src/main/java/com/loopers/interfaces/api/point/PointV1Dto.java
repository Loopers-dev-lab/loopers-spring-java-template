package com.loopers.interfaces.api.point;

import com.loopers.domain.common.vo.Money;

public class PointV1Dto {
    public record PointChargeRequest(long amount) {
        public Money toMoney() {
            return Money.of(amount);
        }
    }

    public record PointResponse(long balance) {
        public static PointResponse from(Money balance) {
            return new PointResponse(balance.getAmount().longValueExact());
        }
    }
}
