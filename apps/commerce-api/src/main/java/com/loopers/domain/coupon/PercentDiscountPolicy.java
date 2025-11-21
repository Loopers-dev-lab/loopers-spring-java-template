package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public class PercentDiscountPolicy implements DiscountPolicy {

    private final int rate;

    public PercentDiscountPolicy(int rate) {
        validate(rate);
        this.rate = rate;
    }

    private void validate(int rate) {
        if (rate < 0 || rate > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 퍼센트는 0 이상 100 이하여야 합니다.");
        }
    }

    @Override
    public int calculateDiscountAmount(int originalPrice) {
        if (originalPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 전 금액은 0 이상이어야 합니다.");
        }

        double discounted = originalPrice * (rate / 100.0);
        return (int) Math.floor(discounted);
    }
}

