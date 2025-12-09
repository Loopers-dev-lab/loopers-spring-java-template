package com.loopers.core.service.payment.event;

import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.vo.PaymentId;

public record PgPaymentEvent(PaymentId paymentId, CouponId couponId) {

}
