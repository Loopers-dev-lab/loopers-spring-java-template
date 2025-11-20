package com.loopers.core.domain.order;

import com.loopers.core.domain.payment.vo.PayAmount;

public interface Coupon {

    PayAmount discount(PayAmount payAmount);

    void use();
}
