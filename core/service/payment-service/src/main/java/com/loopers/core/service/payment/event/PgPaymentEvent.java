package com.loopers.core.service.payment.event;

import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.Payment;

public record PgPaymentEvent(Payment payment, CouponId couponId) {
}
