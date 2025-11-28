package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public class FixedAmountDiscountPolicy implements DiscountPolicy {

    private final int fixedAmount;

    public FixedAmountDiscountPolicy(int fixedAmount) {
        validate(fixedAmount);
        this.fixedAmount = fixedAmount;
    }

    private void validate(int fixedAmount) {
        if (fixedAmount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.");
        }
    }

    @Override
    public int calculateDiscountAmount(int originalPrice) {
        if (originalPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 전 금액은 0 이상이어야 합니다.");
        }
        return fixedAmount;
    }
}

