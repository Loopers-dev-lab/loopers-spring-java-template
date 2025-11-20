package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.vo.CouponId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayAmountDiscountStrategySelector {

    private final NoneDiscountStrategy noneDiscountStrategy;
    private final CouponDiscountStrategy couponDiscountStrategy;

    public PayAmountDiscountStrategy select(CouponId couponId) {
        if (couponId.nonEmpty()) {
            return couponDiscountStrategy;
        }

        return noneDiscountStrategy;
    }
}
