package com.loopers.core.domain.order.repository;

import com.loopers.core.domain.order.Coupon;
import com.loopers.core.domain.order.vo.CouponId;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Coupon getById(CouponId couponId);
}
