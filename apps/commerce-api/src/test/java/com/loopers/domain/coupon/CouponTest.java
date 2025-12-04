package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {
  Coupon coupon;

  @BeforeEach
  void setup() {
    // 기본 AMOUNT 쿠폰 생성
    coupon = CouponFixture.createCouponWith(BigDecimal.valueOf(1000), 3);
  }

  @Nested
  @DisplayName("쿠폰 할인 테스트")
  class Discount {

    @Test
    @DisplayName("AMOUNT 쿠폰은 가격에서 금액 차감")
    void amountDiscount() {
      BigDecimal price = new BigDecimal("5000");

      BigDecimal discounted = coupon.discount(price);

      assertThat(discounted).isEqualTo(price.subtract(coupon.getDiscountAmount()));
    }

    @Test
    @DisplayName("할인 금액이 가격보다 크면 0으로 처리")
    void discountCannotBeNegative() {
      BigDecimal price = new BigDecimal("500");

      BigDecimal discounted = coupon.discount(price);

      assertThat(discounted).isEqualTo(BigDecimal.ZERO);
    }
  }

  @Nested
  @DisplayName("쿠폰 사용 가능 여부")
  class Availability {

    @Test
    @DisplayName("쿠폰 사용 가능")
    void available() {
      assertThat(coupon.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("사용 횟수 초과하면 사용 불가")
    void notAvailableAfterMaxUsage() {
      // maxUsage까지 사용
      for (int i = 0; i < 3; i++) {
        coupon.increaseUsed();
      }

      assertThat(coupon.isAvailable()).isFalse();
    }
  }

  @Nested
  @DisplayName("쿠폰 사용")
  class Use {

    @Test
    @DisplayName("사용 가능 시 increaseUsed 동작")
    void useCouponSuccess() {
      int usedBefore = coupon.getUsedCount().intValue();

      coupon.increaseUsed();

      assertThat(coupon.getUsedCount()).isEqualTo(usedBefore + 1);
    }

    @Test
    @DisplayName("사용 불가 상태에서 사용 시 예외 발생")
    void useCouponFail() {
      // maxUsage 초과
      for (int i = 0; i < 3; i++) coupon.increaseUsed();

      assertThatThrownBy(coupon::increaseUsed) // 실제로는 increaseUsed()는 제한 X, 필요 시 비즈니스 로직으로 예외 처리
          .isInstanceOf(CoreException.class)
          .hasMessageContaining("쿠폰 사용 불가");
    }
  }

}
