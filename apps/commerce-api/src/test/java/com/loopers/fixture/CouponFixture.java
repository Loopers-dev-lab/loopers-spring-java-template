package com.loopers.fixture;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.domain.user.User;

public class CouponFixture {

    // 정액 할인 쿠폰 (기본 5000원)
    public static Coupon fixedAmount(User user) {
        return Coupon.create(user, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
    }

    // 정액 할인 쿠폰 (금액 지정)
    public static Coupon fixedAmount(User user, Long amount) {
        return Coupon.create(user, amount + "원 할인", DiscountType.FIXED_AMOUNT, amount);
    }

    // 정률 할인 쿠폰 (기본 10%)
    public static Coupon percentage(User user) {
        return Coupon.create(user, "10% 할인", DiscountType.PERCENTAGE, 10L);
    }

    // 정률 할인 쿠폰 (비율 지정)
    public static Coupon percentage(User user, Long percent) {
        return Coupon.create(user, percent + "% 할인", DiscountType.PERCENTAGE, percent);
    }

    // 전체 커스텀
    public static Coupon custom(User user, String name, DiscountType type, Long value) {
        return Coupon.create(user, name, type, value);
    }

    // 1000원 할인
    public static Coupon discount1000(User user) {
        return Coupon.create(user, "1000원 할인", DiscountType.FIXED_AMOUNT, 1000L);
    }

    // 20% 할인
    public static Coupon discount20Percent(User user) {
        return Coupon.create(user, "20% 할인", DiscountType.PERCENTAGE, 20L);
    }

    // 50% 할인
    public static Coupon discount50Percent(User user) {
        return Coupon.create(user, "50% 할인", DiscountType.PERCENTAGE, 50L);
    }
}
