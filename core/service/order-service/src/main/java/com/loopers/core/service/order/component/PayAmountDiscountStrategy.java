package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.vo.PayAmount;

public interface PayAmountDiscountStrategy {

    PayAmount discount(PayAmount payAmount, CouponId couponId);
}
