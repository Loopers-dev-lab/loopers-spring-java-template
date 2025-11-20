package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.vo.PayAmount;
import org.springframework.stereotype.Component;

@Component
public class NoneDiscountStrategy implements PayAmountDiscountStrategy {

    @Override
    public PayAmount discount(PayAmount payAmount, CouponId couponId) {
        return payAmount;
    }
}
