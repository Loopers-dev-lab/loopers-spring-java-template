package com.loopers.domain.coupon.fixture;

import com.loopers.domain.coupon.CouponModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponFixture {

    public static final Long DEFAULT_USER_ID = 1L;
    public static final BigDecimal DEFAULT_FIXED_AMOUNT = new BigDecimal("5000");
    public static final BigDecimal DEFAULT_RATE = new BigDecimal("0.1"); // 10%

    public static CouponModel createFixedCoupon() {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createFixed(DEFAULT_USER_ID, DEFAULT_FIXED_AMOUNT, expiredAt);
    }

    public static CouponModel createRateCoupon() {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createRate(DEFAULT_USER_ID, DEFAULT_RATE, expiredAt);
    }

    public static CouponModel createFixedCouponWithUserId(Long userId) {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createFixed(userId, DEFAULT_FIXED_AMOUNT, expiredAt);
    }

    public static CouponModel createFixedCouponWithAmount(BigDecimal amount) {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createFixed(DEFAULT_USER_ID, amount, expiredAt);
    }

    public static CouponModel createRateCouponWithRate(BigDecimal rate) {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createRate(DEFAULT_USER_ID, rate, expiredAt);
    }

    public static CouponModel createRateCouponWithUserId(Long userId) {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createRate(userId, DEFAULT_RATE, expiredAt);
    }

    public static CouponModel createFixedCouponWithUserIdAndAmount(Long userId, BigDecimal amount) {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createFixed(userId, amount, expiredAt);
    }

    public static CouponModel createRateCouponWithUserIdAndRate(Long userId, BigDecimal rate) {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        return CouponModel.createRate(userId, rate, expiredAt);
    }

    public static CouponModel createFixedCouponWithExpiration(LocalDateTime expiredAt) {
        return CouponModel.createFixed(DEFAULT_USER_ID, DEFAULT_FIXED_AMOUNT, expiredAt);
    }

    public static CouponModel createFixedCouponWithIssuedAndExpiredAt(LocalDateTime issuedAt, LocalDateTime expiredAt) {
        return CouponModel.createFixedWithIssueDate(DEFAULT_USER_ID, DEFAULT_FIXED_AMOUNT, issuedAt, expiredAt);
    }

    public static CouponModel createUsedFixedCoupon() {
        CouponModel coupon = createFixedCoupon();
        coupon.use(100L);
        return coupon;
    }

    public static CouponModel createUsedFixedCouponWithUserId(Long userId) {
        CouponModel coupon = createFixedCouponWithUserId(userId);
        coupon.use(100L);
        return coupon;
    }

    public static CouponModel createUsedFixedCouponWithOrderId(Long orderId) {
        CouponModel coupon = createFixedCoupon();
        coupon.use(orderId);
        return coupon;
    }
}
