package com.loopers.domain.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CouponFixture {
  /**
   * 랜덤 Coupon 생성, policyType 지정 가능
   */
  public static Coupon createCoupon(CouponPolicyType policyType) {
    LocalDateTime startAt = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(0, 5));
    LocalDateTime endAt = LocalDateTime.now().plusDays(ThreadLocalRandom.current().nextInt(1, 30));
    long maxUsage = ThreadLocalRandom.current().nextLong(1, 100);

    BigDecimal discountAmount = null;
    Double discountRate = null;

    if (policyType == CouponPolicyType.AMOUNT) {
      discountAmount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(100, 10001));
    } else {
      discountRate = ThreadLocalRandom.current().nextDouble(0.05, 0.5);
    }

    Coupon coupon = Coupon.create(policyType, discountAmount, discountRate, startAt, endAt, maxUsage);

    return coupon;
  }

  /**
   * 기본 Coupon 생성 (AMOUNT)
   */
  public static Coupon createCoupon() {
    return createCoupon(CouponPolicyType.AMOUNT);
  }

  /**
   * 특정 필드만 override
   */
  public static Coupon createCouponWith(BigDecimal discountAmount, long maxUsage) {
    Coupon coupon = Coupon.create(
        CouponPolicyType.AMOUNT,
        discountAmount,
        null,
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now().plusDays(7),
        maxUsage
    );
    return coupon;
  }

  /**
   * 리스트 생성
   */
  public static List<Coupon> createCouponList(int size, CouponPolicyType policyType) {
    List<Coupon> list = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      list.add(createCoupon(policyType));
    }
    return list;
  }
}
