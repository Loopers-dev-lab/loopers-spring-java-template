package com.loopers.core.domain.order;

import com.loopers.core.domain.payment.vo.PayAmount;

import java.math.BigDecimal;

public interface Coupon {

    BigDecimal calculateDiscountAmount(PayAmount payAmount);
}
